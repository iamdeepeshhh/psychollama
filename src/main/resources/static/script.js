let totalPlayers = 0;
let topics = {};
let isReady = false;
let playerName, roomCode, playerId;
let backendUrl = window.location.origin;
let votingPollInFlight = false; // optional: prevents overlapping polls
let activeQuestionKey = ""; // track which question is active to avoid stale updates
let movedToVote = false;      // track whether we've navigated to vote for this question
let questionTimer = null;     // countdown timer handle for question phase
let gameMode = "normal"; // default
let stompClient = null;
let answeredQuestions = new Set();

const loadingMessages = [
  "🎉 PsychOllama is preparing the chaos...",
  "😂 Warming up the llamas...",
  "🔥 Spicing up the questions...",
  "🥳 Almost there, get ready to laugh!",
  "🍕 Ordering extra cheese jokes...",
  "🤪 Making things weird in a good way...",
  "🦙 Our llama is thinking really hard...",
  "🎭 Mixing comedy with chaos...",
  "📢 Llamas whispering the funniest secrets...",
  "💡 Crafting brain-tickling questions...",
  "🎮 Setting the stage for ultimate fun...",
  "🥤 Loading silliness… please hold your soda!",
  "🎶 Playing dramatic background music...",
  "🤔 Wondering why the chicken crossed the road...",
  "📦 Delivering laughter in small packages...",
  "💥 Explosions of jokes loading...",
  "🚀 PsychOllama is taking off...",
  "👑 Polishing the crown for the winner...",
  "📸 Perfecting your share-worthy screenshots...",
  "🥕 Feeding carrots to the llamas...",
  "🔮 Predicting who’s about to be roasted...",
  "🎲 Rolling dice of destiny...",
  "🎯 Aiming jokes at your funny bone...",
  "💻 Debugging your sense of humor...",
  "🥸 Adding fake mustaches to answers...",
  "📚 Reading Dad jokes for inspiration...",
  "🎃 Scaring away boring moments...",
  "🏆 Preparing victory speeches...",
  "🛠 Fixing glitches with duct tape...",
  "💤 Waking up the sleepy llama...",
  "🍿 Cooking popcorn for the show...",
  "📱 Testing memes on social media...",
  "💫 Making your answers 20% funnier...",
  "🎂 Baking a cake of chaos...",
  "🐙 An octopus is helping shuffle questions...",
  "🛸 Aliens beaming down some comedy...",
  "🎨 Painting jokes with extra colors...",
  "🏖 Adding beach vibes to your party...",
  "🕵️ Investigating who’s the funniest...",
  "🎤 Testing the mic: ha-ha 1, ha-ha 2...",
  "🎬 Directing the funniest movie ever...",
  "🎹 Composing laugh tracks...",
  "🤯 Trying to outsmart your answers...",
  "🍭 Handing out sugar rush to players...",
  "🧩 Puzzling together your chaos...",
  "👾 8-bit llamas are coding new jokes..."
];

let usedMessages = [];

function getNextLoadingMessage() {
  if (usedMessages.length === loadingMessages.length) {
    usedMessages = []; // reset when exhausted
  }
  const remaining = loadingMessages.filter(m => !usedMessages.includes(m));
  const msg = remaining[Math.floor(Math.random() * remaining.length)];
  usedMessages.push(msg);
  return msg;
}

function connectSocket() {
  const socket = new SockJS(`${backendUrl}/ws-game`); // ✅ new endpoint
  stompClient = Stomp.over(socket);

  stompClient.connect({}, (frame) => {
    console.log("✅ Connected: " + frame);

    // Subscribe to game state updates for this room
    stompClient.subscribe(`/topic/room/${roomCode}`, (message) => {
      const state = JSON.parse(message.body);
      handleGameState(state);
    });
  });
}

function handleGameState(state) {
  console.log("📩 GameState received:", state);

  currentRound = state.round;
  currentSequence = state.sequence;

  switch (state.phase) {
    case "question":
      activeQuestionKey = `${state.round}:${state.sequence}`;
      showQuestion(state.questionText);
      break;

    case "vote":
      if (state.answers && Array.isArray(state.answers)) {
        loadVoting(state.answers);
      } else {
        console.warn("⚠️ No answers received in GameState", state);
      }
      break;

    case "result":
      if (state.results) {
        renderVoteResults(state.results);
      } else {
        console.warn("⚠️ No results received in GameState", state);
      }
      break;

    case "scoreboard":
      if (state.scores) {
        showScoreboard(state.scores);
      }
      break;

    case "final-scoreboard":
      if (state.scores) showFinalScoreboard(state.scores);
      break;

    default:
      console.warn("⚠️ Unknown game phase:", state.phase);
  }
}


function clearPhaseTimers() {
  if (votingCheckTimeout)  { clearTimeout(votingCheckTimeout); votingCheckTimeout = null; }
  if (questionTimer)       { clearInterval(questionTimer); questionTimer = null; }
}

function showQuestion(text) {
  clearPhaseTimers();
  showScreen("questionScreen");
  document.getElementById("questionText").innerText = text;
  startTimer(5);
}


// game state
let currentRound = 1;
let currentSequence = 1; // 1..N within the round
let currentQuestions = [];
let votingCheckTimeout = null;
let lastSubmittedAnswerText = "";

// Show screens
function showScreen(screenId) {
  let screens = ["lobbyScreen","waitingRoomScreen","questionScreen","answerScreen","voteScreen","resultScreen","scoreScreen","loadingScreen"];
  screens.forEach(id => document.getElementById(id).classList.add("hidden"));
  document.getElementById(screenId).classList.remove("hidden");
}

// Create Room
async function createRoom() {
  playerName = document.getElementById("playerName").value;
  const mode = document.getElementById("gameMode").value;
  const language = document.getElementById("gameLanguage").value;

  if (!playerName) return alert("Enter your name!");

  try {
    const response = await safeFetch(
      `${backendUrl}/room/create?playerName=${encodeURIComponent(playerName)}&mode=${mode}&language=${language}`,
      { method: "POST" }
    );

    if (!response.ok) throw new Error("Room creation failed");

    const data = await response.json();
    const room = data.room;
    const player = data.player;

    roomCode = room.code;
    playerId = player.id;
    gameMode = room.mode;
    connectSocket();

    alert(`Room created! Code: ${roomCode} | Mode: ${room.mode} | Language: ${room.language}`);
    document.getElementById("roomCodeDisplay").innerText = roomCode;

    showScreen("waitingRoomScreen");
    loadPlayers();
  } catch (err) {
    alert(err.message);
  }
}


// Join Room
async function joinGame() {
  playerName = document.getElementById("playerName").value;
  roomCode = document.getElementById("roomCode").value;
  if (!playerName || !roomCode) return alert("Enter name and room code!");

  try {
    const response = await safeFetch(
      `${backendUrl}/player/join?name=${encodeURIComponent(playerName)}&roomCode=${roomCode}`,
      { method: "POST" }
    );
    if (!response.ok) throw new Error("Failed to join room");

    const data = await response.json();
    const player = data.player;
    const room = data.room;

    playerId = player.id;
    roomCode = room.code;
    gameMode = room.mode;
    connectSocket();
    // 🎨 Apply theme
    applyTheme(room.mode);

    document.getElementById("roomCodeDisplay").innerText = roomCode;
    showScreen("waitingRoomScreen");
    loadPlayers();
  } catch (err) {
    alert(err.message);
  }
}


// Fetch all players in room
async function loadPlayers() {
  if (!roomCode) return;
  try {
    const response = await safeFetch(`${backendUrl}/player/${roomCode}`);
    if (response.ok) {
      const players = await response.json();
      totalPlayers = players.length;   // ✅ update count here
      let list = document.getElementById("playersList");
      list.innerHTML = "";
      players.forEach(p => {
        let div = document.createElement("div");
        div.className = "player-entry";
        div.innerText = p.name;
        list.appendChild(div);
      });
    }
  } catch (err) {
    console.error("Failed to load players", err);
  }
}


// Topic submit + ready
// Topic submit + auto-ready
async function submitTopic() {
  let t = document.getElementById("playerTopic").value;
  if (!t) return alert("Enter a topic!");

  try {
    const response = await safeFetch(
      `${backendUrl}/topic/submit?playerId=${playerId}&roomCode=${roomCode}&name=${encodeURIComponent(t)}`,
      { method: "POST" }
    );
    if (!response.ok) throw new Error("Failed to submit topic");

    const topic = await response.json();
    console.log("✅ Topic submitted:", topic);

    // Hide / disable topic UI
    const topicBtn = document.getElementById("topicSubmitBtn");
    if (topicBtn) topicBtn.style.display = "none";
    const topicInput = document.getElementById("playerTopic");
    if (topicInput) topicInput.disabled = true;

    // 🔑 Enable Ready button AFTER topic is submitted
    const readyBtn = document.getElementById("readyBtn");
    if (readyBtn) readyBtn.disabled = false;

    loadPlayers();

  } catch (err) {
    console.error(err);
    alert(err.message);
  }
}


async function readyUp() {
  try {
    const res = await safeFetch(`${backendUrl}/player/${playerId}/ready`, { method: "POST" });
    if (!res.ok) throw new Error("Failed to mark ready");

    isReady = true;
    document.getElementById("waitingStatus").innerText = "Waiting for all players...";
    const readyBtn = document.getElementById("readyBtn");
    if (readyBtn) readyBtn.style.display = "none";

    // 🔁 Poll until all players are ready
    const interval = setInterval(async () => {
      try {
        const resp = await safeFetch(`${backendUrl}/player/${roomCode}/all-ready`);
        if (!resp.ok) throw new Error("Error checking ready status");

        const allReady = await resp.json();
        if (allReady) {
          clearInterval(interval);

          // 👇 now that ALL are ready → start polling starter questions
          let countdown = 15;
          document.getElementById("waitingStatus").innerText =
            `All players ready! Starting in ${countdown}s...`;

          const timer = setInterval(async () => {
            countdown--;
            document.getElementById("waitingStatus").innerText =
              `All players ready! Starting in ${countdown}s...`;

            if (countdown <= 0) {
              clearInterval(timer);

              // Show loading screen ONLY before first round
              if (currentRound === 1) {
                document.getElementById("waitingStatus").innerText = "All players ready!";
                showLoadingScreen()

                // ⏳ Small delay to show animation, then start game
                setTimeout(() => {
                  startGame(1);
                }, 3000);
              } else {
                startGame(currentRound); // directly go if not round 1
              }
            }
          }, 1000);
        }
      } catch (err) {
        console.error(err);
      }
    }, 2000);

  } catch (err) {
    alert("Error: " + err.message);
  }
}


// Start Game
function startGame(round = 1) {
  currentRound = round;
  currentSequence = 1;

  // Instead of fetching, just ask server to start
  stompClient.send(`/app/room/${roomCode}/round/${round}/start`, {});

  // Server will broadcast first question as GameState with phase="question"
}




// Timer
function startTimer(sec){
  let left=sec;
  document.getElementById("timeLeft").innerText=left;
  if (questionTimer) { clearInterval(questionTimer); questionTimer = null; }
  questionTimer = setInterval(()=>{
    left--;
    document.getElementById("timeLeft").innerText=left;
    if(left<=0){ clearInterval(questionTimer); questionTimer = null; goToAnswer(); }
  },1000);
}

// Answer phase
function goToAnswer() {
  if (questionTimer) { clearInterval(questionTimer); questionTimer = null; }
  const currentQuestion = document.getElementById("questionText").innerText;
  document.getElementById("answerQuestionText").innerText = currentQuestion;

  showScreen("answerScreen");
  // reset and show controls for this question
  const input = document.getElementById("answerInput");
  const btn = document.querySelector(".submit-btn");
  input.disabled = false;
  input.style.display = "";
  input.value = "";
  if (btn) { btn.disabled = false; btn.style.display = ""; }
  input.focus(); // auto-focus
}

async function submitAnswer() {
  const ans = document.getElementById("answerInput").value;
  if (!ans) return alert("Enter your funny answer!");

  try {
    const resp = await safeFetch(
      `${backendUrl}/answers/submit?playerId=${playerId}&roomCode=${roomCode}&round=${currentRound}&sequence=${currentSequence}&text=${encodeURIComponent(ans)}`,
      { method: "POST" }
    );
    if (!resp.ok) throw new Error("Failed to submit answer");

    answeredQuestions.add(`${currentRound}-${currentSequence}-${playerId}`);

    // ✅ Just show waiting screen
    document.getElementById("waitingStatus").innerText = "Waiting for others to answer...";
    showScreen("waitingRoomScreen");

    // 🔁 Start polling for all-answered
    startAllAnsweredPolling();

  } catch (err) {
    alert(err.message);
  }
}

let allAnsweredInterval = null;

function startAllAnsweredPolling() {
  if (allAnsweredInterval) clearInterval(allAnsweredInterval);

  allAnsweredInterval = setInterval(async () => {
    try {
      const resp = await safeFetch(
        `${backendUrl}/answers/all-answered?roomCode=${roomCode}&round=${currentRound}&sequence=${currentSequence}`
      );
      if (!resp.ok) return;

      const done = await resp.json();
      if (done) {
        clearInterval(allAnsweredInterval);
        allAnsweredInterval = null;
        // ✅ You don’t need to force move client — server will broadcast "vote"
      }
    } catch (err) {
      console.warn("Polling failed:", err);
    }
  }, 2000);
}

// Voting phase
async function loadVoting(answers) {
  if (!Array.isArray(answers)) {
    console.error("❌ No answers array in state", answers);
    return;
  }

  const list = document.getElementById("answersList");
  list.innerHTML = "";

  // 🔑 extra step: check if current player already voted
  try {
    const resp = await safeFetch(
      `${backendUrl}/vote/already-voted?playerId=${playerId}&roomCode=${roomCode}&round=${currentRound}&sequence=${currentSequence}`
    );
    const alreadyVoted = await resp.json();

    answers.forEach(a => {
      const btn = document.createElement("button");
      btn.innerText = a.text + (a.playerId === playerId ? " (you)" : "");

      if (alreadyVoted) {
        btn.disabled = true;
        btn.title = "You already voted!";
      } else if (a.playerId === playerId && gameMode !== "dating") {
        btn.disabled = true;
        btn.title = "You can't vote for yourself";
      } else {
        btn.onclick = () => vote(a.id);
      }

      list.appendChild(btn);
    });

    showScreen("voteScreen");
  } catch (err) {
    console.error("⚠️ Failed to check already-voted", err);
  }
}

// Vote
async function vote(answerId) {
  try {
    const resp = await safeFetch(
      `${backendUrl}/vote?voterId=${playerId}&answerId=${answerId}&roomCode=${roomCode}`,
      { method: "POST" }
    );
    if (!resp.ok) {
      try {
        const errJson = await resp.json();
        throw new Error(errJson.message || "Vote failed");
      } catch (_) {
        throw new Error("Vote failed");
      }
    }

    // ✅ Show waiting screen only
    document.getElementById("funniestAnswer").innerText =
      gameMode === "dating"
        ? "Thanks! Waiting for results..."
        : "Thanks! Waiting for everyone to vote...";

    showScreen("resultScreen");

    // ❌ remove polling — backend will broadcast GameState with phase="result"
  } catch (err) {
    alert(err.message);
  }
}

function renderVoteResults(data) {
  console.log(`✅ Vote finished for Q${currentSequence}`);

  const winner = [...data.byAnswer].sort((a, b) => b.votes - a.votes)[0];
  const title = winner
    ? `Winner: “${winner.text}” (${winner.playerName}) — ${winner.votes} vote(s)`
    : "No votes cast";
  document.getElementById("funniestAnswer").innerText = title;

  drawVotePie("voteChart", data.byAnswer);

  const bd = document.getElementById("voteBreakdown");
  bd.innerHTML = "";

  const maxVotes = Math.max(...data.byAnswer.map(a => a.votes || 0));

  data.byAnswer.forEach(a => {
    const li = document.createElement("div");

    // 🎨 Dynamic colors
    if (a.votes === maxVotes && maxVotes > 0) {
      li.style.color = "green";     // winner highlighted
      li.style.fontWeight = "bold";
    } else if (a.votes === 0) {
      li.style.color = "gray";      // no votes
    } else {
      li.style.color = "black";     // normal
    }

    const voters = (a.voterIds || []).map(v => v.name).join(", ");
    li.textContent = `“${a.text}” by ${a.playerName} — ${a.votes} vote(s)`
                   + (voters ? ` [${voters}]` : "");
    bd.appendChild(li);
  });

  showScreen("resultScreen");
  setTimeout(() => {
   stompClient.send(`/app/room/${roomCode}/round/${currentRound}/results-ack`, {}, JSON.stringify({ playerId }));
  }, 5000);
}

function drawVotePie(canvasId, buckets) {
  const c = document.getElementById(canvasId);
  if (!c) return;
  const ctx = c.getContext("2d");
  ctx.clearRect(0, 0, c.width, c.height);

  const total = Math.max(1, buckets.reduce((s, b) => s + (b.votes || 0), 0));
  let start = -Math.PI / 2;
  const colors = ["#4e79a7","#f28e2b","#e15759","#76b7b2","#59a14f","#edc948","#b07aa1","#ff9da7"];

  buckets.forEach((b, i) => {
    const slice = (b.votes || 0) / total * Math.PI * 2;

    // slice
    ctx.beginPath();
    ctx.moveTo(c.width / 2, c.height / 2);
    ctx.arc(c.width / 2, c.height / 2, Math.min(c.width, c.height) / 2 - 5, start, start + slice);
    ctx.closePath();
    ctx.fillStyle = colors[i % colors.length];
    ctx.fill();

    // ✅ Add label in the middle of slice
    if (b.votes > 0) {
      const mid = start + slice / 2;
      const x = c.width / 2 + Math.cos(mid) * (c.width / 3);
      const y = c.height / 2 + Math.sin(mid) * (c.height / 3);

      // choose contrasting text color
      ctx.fillStyle = "#000";
      ctx.font = "12px Arial";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(`${b.playerName} (${b.votes})`, x, y);
    }

    start += slice;
  });
}




// Scoreboard
function showScoreboard(scores) {
  showScreen("scoreScreen");

  const board = document.getElementById("scoreBoard");
  board.innerHTML = "";

  scores.forEach(s => {
    const li = document.createElement("li");
    li.textContent = `${s.playerName}: ${s.points} pts`;
    board.appendChild(li);
  });
}


// Next round
function nextRound() {
  startGame(currentRound + 1);
}

function applyTheme(mode) {
  document.body.classList.remove("normal-mode", "dating-mode", "officeparty-mode", "houseparty-mode");
  document.body.classList.add(`${mode}-mode`);
}

function changeMode() {
    const mode = document.getElementById("gameMode").value;
    const body = document.body;
    const tagline = document.getElementById("tagline");

    // Remove previous mode classes
    body.classList.remove("normal-mode", "dating-mode", "officeparty-mode", "houseparty-mode");

    // Add the new mode class
    body.classList.add(`${mode}-mode`);

    // Change tagline based on mode
    switch (mode) {
        case "normal":
            tagline.textContent = "Write the funniest answers. Fool your friends. Win the crown!";
            break;
        case "dating":
            tagline.textContent = "💘 Break the ice, make them laugh, win their heart!";
            break;
        case "officeparty":
            tagline.textContent = "🏢 Roast your coworkers (nicely) and grab the crown!";
            break;
        case "houseparty":
            tagline.textContent = "🏠 Bring the chaos, outwit your friends, rule the night!";
            break;
    }
}

async function safeFetch(url, options = {}, retries = 3, delay = 2000) {
  try {
    const resp = await fetch(url, options);
    if (!resp.ok) throw new Error(`HTTP ${resp.status} on ${url}`);
    return resp;
  } catch (err) {
    console.warn(`⚠️ Fetch failed: ${url}`, err);

    if (retries > 0) {
      console.log(`🔄 Retrying in ${delay / 1000}s... (${retries} left)`);
      await new Promise(r => setTimeout(r, delay));
      return safeFetch(url, options, retries - 1, delay);
    }

    // After all retries, show reconnect message
    const waiting = document.getElementById("waitingStatus");
    if (waiting) waiting.innerText = "⚠️ Connection lost. Trying to reconnect...";
    throw err;
  }
}

function shareResult() {
  const text = document.getElementById("funniestAnswer").innerText;
  if (navigator.share) {
    navigator.share({
      title: "PsychOllama",
      text: `😂 ${text} #PsychOllama`,
      url: window.location.href
    });
  } else {
    alert("Copy this to share: " + text);
  }
}

// 🎮 Mini emoji game logic
let score = 0;
let emojiInterval;
let spawnSpeed = 1200; // starting interval
let fallSpeed = 3;     // starting CSS seconds

function startMiniGame() {
  const gameArea = document.getElementById("gameArea");
  score = 0;
  document.getElementById("gameScore").innerText = "Score: 0";

  if (emojiInterval) clearInterval(emojiInterval);

  emojiInterval = setInterval(spawnEmoji, spawnSpeed);

  function spawnEmoji() {
    const emoji = document.createElement("div");
    emoji.className = "emoji";
    emoji.innerText = ["😂","🎉","🔥","🍕","🥳","🤪","🦙"][Math.floor(Math.random()*7)];
    emoji.style.left = Math.random() * 200 + "px";
    emoji.style.animationDuration = Math.max(1.5, 5 - score * 0.2) + "s";

    emoji.onclick = () => {
      score++;
      document.getElementById("gameScore").innerText = `Score: ${score}`;
      emoji.remove();

      // ⏩ speed up every 5 points
      if (score % 5 === 0 && spawnSpeed > 400) {
        spawnSpeed -= 100;
        fallSpeed = Math.max(1, fallSpeed - 0.2);
        clearInterval(emojiInterval);
        emojiInterval = setInterval(spawnEmoji, spawnSpeed);
      }
    };

    emoji.addEventListener("animationend", () => emoji.remove());
    gameArea.appendChild(emoji);
  }
}


function stopMiniGame() {
  if (emojiInterval) clearInterval(emojiInterval);
  emojiInterval = null;
  const gameArea = document.getElementById("gameArea");
  if (gameArea) gameArea.innerHTML = ""; // clear emojis when leaving
}



function showLoadingScreen() {
  showScreen("loadingScreen");
  startMiniGame();

  const status = document.querySelector("#loadingScreen p");

  function cycle() {
    status.innerText = getNextLoadingMessage();
  }

  cycle(); // set first message immediately
  const msgInterval = setInterval(cycle, 3000);

  // stop cycling when leaving loading screen
  const observer = new MutationObserver(() => {
    if (document.getElementById("loadingScreen").classList.contains("hidden")) {
      clearInterval(msgInterval);
      stopMiniGame();   // cleanup mini-game too
      observer.disconnect();
    }
  });
  observer.observe(document.body, { attributes: true, subtree: true });
}

function showFinalScoreboard(scores) {
  showScreen("finalScoreScreen"); // switch to final leaderboard screen

  const board = document.getElementById("finalScoreBoard");
  board.innerHTML = "";

  // Sort by cumulative points descending
  scores.sort((a, b) => b.cumulativePoints - a.cumulativePoints);

  scores.forEach((s, index) => {
    const li = document.createElement("li");

    if (index === 0) {
      li.style.fontWeight = "bold";
      li.style.color = "gold";
      li.textContent = `👑 ${s.playerName}: ${s.cumulativePoints} pts`;
    } else {
      li.textContent = `${s.playerName}: ${s.cumulativePoints} pts`;
    }

    board.appendChild(li);
  });

  // Update winner message
  const winnerMsg = document.querySelector("#finalScoreScreen .winner-message");
  if (scores.length > 0) {
    winnerMsg.innerText = `🎉 Congratulations ${scores[0].playerName}, you are the winner!`;
  } else {
    winnerMsg.innerText = "🎉 Game Over! Thanks for playing!";
  }
}
