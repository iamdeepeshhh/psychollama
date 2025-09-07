let totalPlayers = 0;
let topics = {};
let isReady = false;
let playerName, roomCode, playerId;
let backendUrl = window.location.origin;
let voteResultsPoll = null;
let votingPollInFlight = false; // optional: prevents overlapping polls
let activeQuestionKey = ""; // track which question is active to avoid stale updates
let movedToVote = false;      // track whether we've navigated to vote for this question
let questionTimer = null;     // countdown timer handle for question phase

function clearPhaseTimers() {
  if (answerCheckInterval) { clearInterval(answerCheckInterval); answerCheckInterval = null; }
  if (votingCheckTimeout)  { clearTimeout(votingCheckTimeout); votingCheckTimeout = null; }
  if (voteResultsPoll)     { clearInterval(voteResultsPoll); voteResultsPoll = null; }
  if (questionTimer)       { clearInterval(questionTimer); questionTimer = null; }
}

function showCurrentQuestion() {
  clearPhaseTimers();
  const q = currentQuestions.find(q => q.sequence === currentSequence);
  if (!q) return;

  document.getElementById("questionText").innerText = q.text;
  activeQuestionKey = `${currentRound}:${q.sequence}`;
  movedToVote = false;
  startTimer(20);
  showScreen("questionScreen");
}


// game state
let currentRound = 1;
let currentSequence = 1; // 1..N within the round
let currentQuestions = [];
let answerCheckInterval = null;
// let voteResultsPoll declared above; avoid redeclare
let votingCheckTimeout = null;
let lastSubmittedAnswerText = "";

// Show screens
function showScreen(screenId) {
  let screens = ["lobbyScreen","waitingRoomScreen","questionScreen","answerScreen","voteScreen","resultScreen","scoreScreen"];
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
    const response = await fetch(
      `${backendUrl}/room/create?playerName=${encodeURIComponent(playerName)}&mode=${mode}&language=${language}`,
      { method: "POST" }
    );

    if (!response.ok) throw new Error("Room creation failed");

    const data = await response.json();
    const room = data.room;
    const player = data.player;

    roomCode = room.code;
    playerId = player.id;

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
    const response = await fetch(
      `${backendUrl}/player/join?name=${encodeURIComponent(playerName)}&roomCode=${roomCode}`,
      { method: "POST" }
    );
    if (!response.ok) throw new Error("Failed to join room");

    const data = await response.json();
    const player = data.player;
    const room = data.room;

    playerId = player.id;
    roomCode = room.code;

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
    const response = await fetch(`${backendUrl}/player/${roomCode}`);
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
    const response = await fetch(
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
    const res = await fetch(`${backendUrl}/player/${playerId}/ready`, { method: "POST" });
    if (!res.ok) throw new Error("Failed to mark ready");

    isReady = true;
    document.getElementById("waitingStatus").innerText = "Waiting for all players...";
    const readyBtn = document.getElementById("readyBtn");
    if (readyBtn) readyBtn.style.display = "none";

    // 🔁 Poll until all players are ready
    const interval = setInterval(async () => {
      try {
        const resp = await fetch(`${backendUrl}/player/${roomCode}/all-ready`);
        if (!resp.ok) throw new Error("Error checking ready status");

        const allReady = await resp.json();
        if (allReady) {
          clearInterval(interval);

          // 👇 now that ALL are ready → start polling starter questions
          let countdown = 16;
          document.getElementById("waitingStatus").innerText =
            `All players ready! Starting in ${countdown}s...`;

          const timer = setInterval(async () => {
            countdown--;
            document.getElementById("waitingStatus").innerText =
              `All players ready! Starting in ${countdown}s...`;

            if (countdown <= 0) {
              clearInterval(timer);
              // 🟢 begin round 1 → fetch starter questions here
              startGame(1);
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
async function startGame(round = 1) {
  currentRound = round;
  currentSequence = 1;

  if (round === 1) {
    currentQuestions = await fetchStarterQuestions(); // will poll until ready
  } else {
    const resp = await fetch(`${backendUrl}/game/questions/${roomCode}/round/${round}`);
    if (!resp.ok) throw new Error("Failed to fetch questions");
    currentQuestions = await resp.json();
  }

  if (currentQuestions.length > 0) {
    showCurrentQuestion();
  } else {
    alert("No questions available yet!");
  }
}


async function fetchStarterQuestions() {
  let questions = [];
  while (questions.length === 0) {
    try {
      const resp = await fetch(`${backendUrl}/game/starter-questions/${roomCode}`);
      if (resp.ok) {
        questions = await resp.json();
      }
      if (questions.length > 0) {
        console.log("✅ Starter questions ready:", questions);
        return questions;
      }
    } catch (err) {
      console.error("Error fetching starter questions:", err);
    }
    // wait 1.5s before retry
    await new Promise(r => setTimeout(r, 1500));
  }
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
    // freeze UI
    const input = document.getElementById("answerInput");
    const btn = document.querySelector(".submit-btn");
    input.disabled = true; input.style.display = "none";
    if (btn) { btn.disabled = true; btn.style.display = "none"; }
    if (questionTimer) { clearInterval(questionTimer); questionTimer = null; }

    const resp = await fetch(
      `${backendUrl}/answers/submit?playerId=${playerId}&roomCode=${roomCode}&round=${currentRound}&sequence=${currentSequence}&text=${encodeURIComponent(ans)}`,
      { method: "POST" }
    );
    if (!resp.ok) throw new Error("Failed to submit answer");

    lastSubmittedAnswerText = ans;

    // show waiting screen and poll strictly until *all answered*
    document.getElementById("waitingStatus").innerText = "Waiting for others to answer...";
    showScreen("waitingRoomScreen");

    if (answerCheckInterval) clearInterval(answerCheckInterval);
    answerCheckInterval = setInterval(checkIfAllAnswered, 1500);
  } catch (err) {
    alert(err.message);
  }
}




async function checkIfAllAnswered() {
  try {
    const url = `${backendUrl}/answers/all-answered?roomCode=${roomCode}&round=${currentRound}&sequence=${currentSequence}`;
    const resp = await fetch(url);
    if (!resp.ok) throw new Error("Check failed");

    const allAnswered = await resp.json();
    if (allAnswered) {
      if (answerCheckInterval) { clearInterval(answerCheckInterval); answerCheckInterval = null; }
      loadVoting(); // go render voting UI now
    }
  } catch (err) {
    console.error("Error checking answers:", err);
  }
}



// Voting phase
async function loadVoting() {
  try {
    const key = `${currentRound}:${currentSequence}`;
    if (key !== activeQuestionKey) return; // safety

    movedToVote = true;

    const resp = await fetch(
      `${backendUrl}/answers/list?roomCode=${roomCode}&round=${currentRound}&sequence=${currentSequence}`
    );
    if (!resp.ok) throw new Error("Failed to load answers");

    const answers = await resp.json();
    const list = document.getElementById("answersList");
    list.innerHTML = "";

    // Show all answers; disable self-vote
    answers.forEach(a => {
      const btn = document.createElement("button");
      btn.innerText = a.text + (a.playerId === playerId ? " (you)" : "");
      if (a.playerId === playerId) {
        btn.disabled = true;
        btn.title = "You can't vote for yourself";
      } else {
        btn.onclick = () => vote(a.id);
      }
      list.appendChild(btn);
    });

    showScreen("voteScreen");
  } catch (err) {
    alert(err.message);
  }
}


// Vote
async function vote(answerId) {
  try {
    const resp = await fetch(`${backendUrl}/vote?voterId=${playerId}&answerId=${answerId}`, { method: "POST" });
    if (!resp.ok) {
      try {
        const errJson = await resp.json();
        throw new Error(errJson.message || "Vote failed");
      } catch (_) {
        throw new Error("Vote failed");
      }
    }

    // Show waiting-for-votes screen
    document.getElementById("funniestAnswer").innerText = "Thanks! Waiting for everyone to vote...";
    showScreen("resultScreen");

    // Poll until everyone voted
    if (voteResultsPoll) clearInterval(voteResultsPoll);
    const thisKey = activeQuestionKey;
    voteResultsPoll = setInterval(async () => {
      try {
        if (thisKey !== activeQuestionKey) { clearInterval(voteResultsPoll); voteResultsPoll = null; return; }
        // (Optional) keep players fresh so expected is accurate
        await loadPlayers();

        const res = await fetch(`${backendUrl}/vote/all-voted?roomCode=${roomCode}&round=${currentRound}&sequence=${currentSequence}`);
        if (!res.ok) throw new Error("all-voted check failed");
        const allVoted = await res.json();

        if (allVoted) {
          clearInterval(voteResultsPoll);
          voteResultsPoll = null;
          await renderVoteResults(); // draw pie + who voted
        }
      } catch (e) {
        console.error(e);
      }
    }, 1500);
  } catch (err) {
    alert(err.message);
  }
}


async function renderVoteResults() {
  try {
    const resp = await fetch(`${backendUrl}/vote/results?roomCode=${roomCode}&round=${currentRound}&sequence=${currentSequence}`);
    if (!resp.ok) throw new Error("Failed to load vote results");
    const data = await resp.json();
    // data = { expectedVoters, totalVotes, byAnswer: [{answerId, text, playerId, playerName, votes, voterIds: [{id,name}]}], voters: [{voterId, voterName, votedAnswerId}] }

    // Headline: winner
    const winner = [...data.byAnswer].sort((a,b) => b.votes - a.votes)[0];
    const title = winner
      ? `Winner: “${winner.text}” (${winner.playerName}) — ${winner.votes} vote(s)`
      : "No votes cast";
    document.getElementById("funniestAnswer").innerText = title;

    // Draw pie
    drawVotePie("voteChart", data.byAnswer);

    // Breakdown list (who voted for whom)
    const bd = document.getElementById("voteBreakdown");
    if (bd) {
    bd.innerHTML = "";
    data.byAnswer.forEach(a => {
      const li = document.createElement("div");
      const voters = (a.voterIds || []).map(v => v.name).join(", ");
      li.textContent = `“${a.text}” by ${a.playerName} — ${a.votes} vote(s)` + (voters ? ` [${voters}]` : "");
      bd.appendChild(li);
    });
    }

    // Proceed after a short pause (or show a “Next” button of your choice)
    setTimeout(() => {
      if (currentSequence < currentQuestions.length) {
        currentSequence += 1;
        showCurrentQuestion();
      } else {
        showScoreboard();
      }
    }, 5000);
  } catch (err) {
    console.error(err);
  }
}

function drawVotePie(canvasId, buckets) {
  const c = document.getElementById(canvasId);
  if (!c) return; // ensure canvas exists in your resultScreen
  const ctx = c.getContext("2d");
  ctx.clearRect(0,0,c.width,c.height);

  const total = Math.max(1, buckets.reduce((s,b)=>s + (b.votes||0), 0));
  let start = -Math.PI/2;
  // simple colors; you can style better
  const colors = ["#4e79a7","#f28e2b","#e15759","#76b7b2","#59a14f","#edc948","#b07aa1","#ff9da7"];

  buckets.forEach((b, i) => {
    const slice = (b.votes || 0) / total * Math.PI * 2;
    ctx.beginPath();
    ctx.moveTo(c.width/2, c.height/2);
    ctx.arc(c.width/2, c.height/2, Math.min(c.width,c.height)/2 - 5, start, start + slice);
    ctx.closePath();
    ctx.fillStyle = colors[i % colors.length];
    ctx.fill();
    start += slice;
  });

  // optional: add labels as legend under the canvas — we already list them in voteBreakdown
}



// Scoreboard
async function showScoreboard() {
  showScreen("scoreScreen");

  try {
    const resp = await fetch(`${backendUrl}/game/scoreboard/${roomCode}`);
    if (!resp.ok) throw new Error("Failed to fetch scoreboard");

    const scores = await resp.json();
    const board = document.getElementById("scoreBoard");
    board.innerHTML = "";

    scores.forEach(s => {
      const li = document.createElement("li");
      li.textContent = `${s.playerName}: ${s.points} pts`;
      board.appendChild(li);
    });
  } catch (err) {
    console.error(err);
    document.getElementById("scoreBoard").innerHTML = "<li>Error loading scores</li>";
  }
}


// Next round
function nextRound() {
  startGame(currentRound + 1);
}

function applyTheme(mode) {
  document.body.classList.remove("normal-mode", "dating-mode", "officeparty-mode", "houseparty-mode");
  document.body.classList.add(`${mode}-mode`);
}

