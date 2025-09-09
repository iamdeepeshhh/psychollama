package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Question;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Entity.Topic;
import com.Chanalyst.ChanalystV1.Repository.PlayerRepository;
import com.Chanalyst.ChanalystV1.Repository.QuestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionGeneratorService {

    private final RestTemplateBuilder restTemplate;
    private final RoomService roomService;
    private final PlayerRepository playerRepository;
    private final TopicService topicService;
    private final QuestionRepository questionRepository;

    private static final String OLLAMA_URL = "http://127.0.0.1:11434/api/generate";
    private static final String MODEL = "llama3.2:3b";

    /**
     * Explicit round-based question generation
     */
    public List<Question> generateStarterQuestions(String roomCode, String mode, int round, String language) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found!"));

        List<Player> players = playerRepository.findByRoom(room);
        List<Topic> topics = topicService.getTopicsByRoom(room);

        String playerStr = players.stream().map(Player::getName).collect(Collectors.joining(", "));
        String topicStr = topics.stream().map(Topic::getName).collect(Collectors.joining(", "));

        List<String> pastQuestions = questionRepository.findByRoom(room)
                .stream()
                .map(Question::getText)
                .toList();

        String pastQStr = String.join("; ", pastQuestions);

        // 👇 Generate exactly 2 starter questions
        String prompt = buildPrompt(mode, 2, round, playerStr, topicStr, pastQStr, language);

        Map<String, Object> request = Map.of(
                "model", MODEL,
                "prompt", prompt,
                "stream", false
        );

        try {
            ResponseEntity<Map> response = restTemplate.build()
                    .postForEntity(OLLAMA_URL, request, Map.class);

            String raw = Objects.requireNonNull(response.getBody()).get("response").toString();

            List<String> questionTexts = Arrays.stream(raw.split("\\d+[.)-]"))
                    .map(String::trim)
                    .filter(q -> !q.isEmpty())
                    .filter(q -> q.endsWith("?"))   // ✅ only keep real questions
                    .toList();

            List<Question> saved = new ArrayList<>();
            for (String text : questionTexts) {
                int baseSeq = questionRepository.findByRoomAndRoundOrderBySequenceAsc(room, round)
                        .stream()
                        .mapToInt(Question::getSequence)
                        .max()
                        .orElse(0);

                if (baseSeq >= 5) break; // no more than 5

                Question q = new Question();
                q.setText(text);
                q.setRoom(room);
                q.setRound(round);
                q.setSequence(baseSeq + 1); // 🚀 keep sequence continuous

                saved.add(questionRepository.saveAndFlush(q));
            }
            return saved;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }


    public void generateQuestions(String roomCode, int count, String mode, int round, String language) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found!"));

        List<Player> players = playerRepository.findByRoom(room);
        List<Topic> topics = topicService.getTopicsByRoom(room);

        String playerStr = players.stream().map(Player::getName).collect(Collectors.joining(", "));
        String topicStr = topics.stream().map(Topic::getName).collect(Collectors.joining(", "));

        // Past questions (for duplicate avoidance)
        List<String> pastQuestions = questionRepository.findByRoom(room)
                .stream()
                .map(Question::getText)
                .toList();

        String pastQStr = String.join("; ", pastQuestions);

        // Build prompt with difficulty scaling
        String prompt = buildPrompt(mode, count, round, playerStr, topicStr, pastQStr, room.getLanguage());

        Map<String, Object> request = Map.of(
                "model", MODEL,
                "prompt", prompt,
                "stream", false
        );

        try {
            ResponseEntity<Map> response = restTemplate.build()
                    .postForEntity(OLLAMA_URL, request, Map.class);

            String raw = Objects.requireNonNull(response.getBody()).get("response").toString();

            List<String> questionTexts = Arrays.stream(raw.split("\\d+[.)-]"))
                    .map(String::trim)
                    .filter(q -> !q.isEmpty())
                    .toList();

            // 🔑 find the current max sequence for this round
            int baseSeq = questionRepository.findByRoomAndRoundOrderBySequenceAsc(room, round)
                    .stream()
                    .mapToInt(Question::getSequence)
                    .max()
                    .orElse(0);

            for (String text : questionTexts) {
                if (!isValidQuestion(text)) continue;
                if (isDuplicate(text, pastQuestions)) continue;
                if (baseSeq >= 5) break; // cap at 5 per round

                baseSeq++; // increment from last saved

                Question q = new Question();
                q.setText(text);
                q.setRoom(room);
                q.setRound(round);
                q.setSequence(baseSeq);

                questionRepository.save(q);

                System.out.println("✅ Saved Q" + q.getSequence() + " for Round " + round + ": " + text);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prompt with round-based difficulty
     */
    private String buildPrompt(String mode, int count, int round, String players, String topics, String pastQStr, String language) {
        // 🌐 Language toggle
        String langInstruction = (language != null && language.equalsIgnoreCase("hindi"))
                ? "Write ONLY in playful Hindi. Keep it short, cheeky, and LOL-worthy. Never ask for reasons. Never use 'अगर %s ... होता' style metaphors."
                : "Write ONLY in playful English. Keep it short, cheeky, and LOL-worthy. Never ask for reasons. Never use 'If X was Y' style metaphors.";

        // 🎯 Round style
        String roundStyle = switch (round) {
            case 1 -> "Round 1 (HOOK): Simple icebreakers, quick laughs, light roasts.";
            case 2 -> "Round 2 (PLAYFUL): Go funnier, sillier, with social chaos energy.";
            default -> "Round 3+ (CHAOS): Bold, absurd, roast-style humor. Push creativity hard, but keep it safe.";
        };

        // 🎭 Mode style
        String modeStyle = switch (mode.toLowerCase()) {
            case "dating" -> "MODE = DATING: Flirty, cheeky, playful. Rom-com banter, no vulgarity.";
            case "officeparty" -> "MODE = OFFICE PARTY: PG-13, goofy corporate humor, awkward-funny workplace vibes.";
            case "houseparty" -> "MODE = HOUSE PARTY: Chill, goofy, inclusive roast style.";
            default -> "MODE = CLASSIC: Silly, witty, punchline humor. Keep it random and fun.";
        };

        // 🧩 Rules
        String rules = """
        RULES:
        - No politics, religion, or sensitive stuff.
        - Never ask 'why' or 'reason'.
        - Never use 'If X was Y' style metaphors.
        - Always use player names: %s
        - Use given topics for inspiration: %s
        - Keep it direct, punchy, and instantly funny.
        - Think like a stand-up comedian hosting a party.
        - Don't make grammatical mistakes.
        """.formatted(players, topics);

        // 🌟 Examples by mode
        String examples;
        if (language != null && language.equalsIgnoreCase("hindi")) {
            examples = switch (mode.toLowerCase()) {
                case "dating" -> """
                Example Hindi (Dating):
                1. सबसे पहले कौन “आई लव यू” बोलकर खुद ही हंस देगा?
                2. कौन सा खिलाड़ी डेट पर सेल्फी लेने में टाइम पास करेगा?
                3. सबसे पहले किसका फोन डेट पर बजेगा?
                """;
                case "officeparty" -> """
                Example Hindi (Office Party):
                1. सबसे पहले कौन बॉस की नकल करेगा?
                2. किसका हंसी मीटिंग में सबसे जोर से फूटेगा?
                3. कौन सा खिलाड़ी कैंटीन से सबसे ज्यादा स्नैक्स उठाएगा?
                """;
                case "houseparty" -> """
                Example Hindi (House Party):
                1. सबसे पहले कौन DJ से “भाई भजन बजा” कहेगा?
                2. किसका फोन हमेशा 1% पर रहेगा?
                3. कौन सा खिलाड़ी गोलगप्पे की लाइन तोड़ेगा?
                """;
                default -> """
                Example Hindi (Classic Party):
                1. किसका ringtone अभी भी “Hello Moto” वाला होगा?
                2. कौन WhatsApp ग्रुप में गलत फोटो भेजेगा?
                3. किसका चेहरा देखकर लगेगा जैसे अभी होमवर्क भूल गया है?
                """;
            };
        } else {
            examples = switch (mode.toLowerCase()) {
                case "dating" -> """
                Example English (Dating):
                1. Who would 100% text back with just a heart emoji?  
                2. Which player would plan a date at McDonald’s?  
                3. Who is most likely to blush before even saying hello?  
                """;
                case "officeparty" -> """
                Example English (Office Party):
                1. Who would send an email with “Reply All” by mistake?  
                2. Which player would laugh in the boss’s serious speech?  
                3. Who is most likely to steal all the samosas at the party?  
                """;
                case "houseparty" -> """
                Example English (House Party):
                1. Who would start karaoke after two drinks?  
                2. Which player would hide snacks and eat them later?  
                3. Who is most likely to trip while dancing?  
                """;
                default -> """
                Example English (Classic Party):
                1. Who is most likely to argue with Siri and lose?  
                2. Which player would forget their own birthday party?  
                3. Who looks like they’d join the wrong Zoom call?  
                """;
            };
        }

        // ✅ Final assembly
        return """
        Generate %d UNIQUE, hilarious one-liner PARTY QUESTIONS.

        %s
        %s
        %s
        %s
        %s

        EXTRA REQUIREMENTS:
        - Each question ≤ 20 words.
        - EVERY question MUST be meaningful, relevant, and understandable.
        - No filler, no nonsense, no repeated patterns.
        - Each question should be funny but still make sense in a party game.
        - At least 2 questions must mention BOTH players: %s.
        - At least 1 must use topics: %s.
        - Cover variety: (1 food, 1 relationship, 1 pop culture, 1 embarrassing habit, 1 random chaos).
        - Use exaggeration, sarcasm, and playful roasting.
        - Questions must feel shareable, like viral memes.

        OUTPUT RULES:
        - Output ONLY the numbered list of questions.
        - Do NOT write anything else — no introductions, no explanations, no headings.
        - Do NOT write "Here are", "Okay", "Sure", or any filler text.
        - The very first character of the response MUST be "1".
        - Each line MUST end with a "?".
        - questions should be meaningful.
        - Every line MUST be a valid question, not a statement with a "?" added.
        - Each question must clearly ask "who", "what", "which", "can", "would", etc.
        - Do NOT output broken grammar or incomplete thoughts.
        - Generate exactly %d questions, numbered 1 to %d.
        """.formatted(
                count, modeStyle, roundStyle, rules, langInstruction, examples, players, topics, count, count
        );

    }

    /**
     * Duplicate detection with Levenshtein similarity
     */
    private boolean isDuplicate(String newQ, List<String> past) {
        LevenshteinDistance distance = LevenshteinDistance.getDefaultInstance();
        for (String oldQ : past) {
            int dist = distance.apply(newQ.toLowerCase(), oldQ.toLowerCase());
            int maxLen = Math.max(newQ.length(), oldQ.length());
            double similarity = 1 - (double) dist / maxLen;
            if (similarity > 0.85) return true;
        }
        return false;
    }

    public List<Question> getQuestionsByRoomAndRound(String roomCode, int round) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found!"));
        return questionRepository.findByRoomAndRoundOrderBySequenceAsc(room, round);
    }

    public void clearQuestions(String roomCode) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found!"));
        questionRepository.deleteByRoom(room);
    }

    private boolean isValidQuestion(String q) {
        // Must end with "?"
        if (!q.endsWith("?")) return false;

        // Must start with a question word or style
        String lower = q.toLowerCase();
        String[] validStarts = {"who", "what", "which", "when", "where", "would", "could", "can", "is", "are", "do", "does"};
        boolean startsValid = Arrays.stream(validStarts).anyMatch(lower::startsWith);

        // Minimum length (avoid super short junk like "Ok?")
        boolean longEnough = q.length() > 10;

        return startsValid && longEnough;
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkAndGenerateStarters() {
        List<Room> rooms = roomService.findAllRooms();
        for (Room room : rooms) {
            // skip if already has questions for round 1
            boolean hasQuestions = !questionRepository
                    .findByRoomAndRoundOrderBySequenceAsc(room, 1).isEmpty();

            if (!hasQuestions) {
                long topicCount = topicService.getTopicsByRoom(room).size();
                if (topicCount > 0) {
                    System.out.println("🚀 Generating starter questions for room " + room.getCode());
                    generateQuestions(room.getCode(),1 , room.getMode(), 1, room.getLanguage());
                }
            }
        }
    }
}

