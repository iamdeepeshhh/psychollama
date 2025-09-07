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
                ? "Write ALL questions in fluent, natural, conversational Hindi. Keep them short, funny, and engaging."
                : "Write ALL questions in fluent, natural, conversational English. Keep them short, funny, and engaging.";

        // 🎯 Round-based difficulty & style
        String levelInstruction = switch (round) {
            case 1 -> """
                  Round 1 = The HOOK round. 
                  - Questions must be highly CREATIVE, witty, and attention-grabbing.
                  - They should be simple enough to answer quickly, but imaginative enough to spark laughter.
                  - Formats to prefer: funny comparisons, emoji metaphors, “who is most likely to…”, or quick imagination prompts.
                  """;
            case 2 -> """
                  Round 2 = The PLAYFUL progression. 
                  - Make questions more daring and playful. 
                  - Encourage funny exaggerations, “what if” scenarios, and humorous storytelling.
                  - Players should feel challenged to be more creative.
                  """;
            default -> """
                  Round 3+ = The WILD rounds. 
                  - Push creativity to the maximum. 
                  - Questions should be bold, unexpected, and hilarious. 
                  - Use exaggerations, friendly roasts, and absurd but safe scenarios.
                  - These rounds should create the loudest laughs of the game.
                  """;
        };

        // 📜 Universal rules
        String baseRules = """
            ⚠️ IMPORTANT RULES:
            - Do NOT end questions with "... and why?". Keep answers short & funny, not essays.
            - Avoid political, religious, or sensitive topics.
            - Do not repeat or rephrase past questions: %s
            - Always involve or reference the players by name: %s
            - Use the given topics for inspiration: %s
            - Keep tone light, humorous, and safe for all players.
            - Output format: strictly a numbered list (1, 2, 3…).
            """.formatted(pastQStr, players, topics);

        // ✨ Mode-specific style
        String modeInstruction = switch (mode.toLowerCase()) {
            case "dating" -> """
                         MODE = DATING GAME
                         - Questions should be romantic, flirty, and playful. 
                         - Focus on chemistry, fun banter, and personality-based humor.
                         - Keep it sweet, cheeky, never offensive.
                         """;
            case "officeparty" -> """
                              MODE = OFFICE PARTY
                              - Questions should be lighthearted icebreakers. 
                              - Safe for workplace, but not boring. 
                              - Professional yet funny, making colleagues laugh together.
                              - Avoid politics/religion, but allow fun exaggerations.
                              """;
            case "houseparty" -> """
                             MODE = HOUSE PARTY
                             - Questions should be casual, inclusive, and fun for mixed groups. 
                             - Encourage friendly banter and imaginative scenarios. 
                             - Keep tone relaxed and social.
                             """;
            default -> """
                   MODE = CLASSIC PARTY
                   - Funny, silly, slightly roasty questions.
                   - Aim to make players laugh instantly.
                   - Keep it casual, witty, and highly engaging.
                   """;
        };

        // ✅ Examples (different for Hindi/English)
        String examples = (language != null && language.equalsIgnoreCase("hindi"))
                ? """
              ✅ Example Hindi Questions:
              1. अगर %s किसी बॉलीवुड फिल्म का हीरो होता, तो कौन सी फिल्म होती?
              2. किस खिलाड़ी का हंसना इतना मज़ेदार है कि बाकी सब भी हंस पड़ें?
              3. अगर %s एक इमोजी होता, तो कौन सा इमोजी होता?
              """.formatted(players, players)
                : """
              ✅ Example English Questions:
              1. If %s were the lead in a movie, which movie would it be?
              2. Whose laugh is so contagious that it could start a chain reaction?
              3. If %s was an emoji, which emoji would they be?
              """.formatted(players, players);

        // 🏗 Final assembly
        return """
            Generate %d UNIQUE, creative, and funny questions for a party game.

            %s

            %s

            %s

            %s

            %s

            OUTPUT:
            - Strictly a numbered list from 1 to %d
            - No explanations, no extra text — ONLY the questions.
            """.formatted(count, modeInstruction, baseRules, levelInstruction, langInstruction, examples, count);
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

