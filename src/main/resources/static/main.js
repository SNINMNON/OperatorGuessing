const gameDiv = document.getElementById("game");
const API_BASE = "http://localhost:8080/api";

function createGameUI() {
gameDiv.innerHTML = `
  <input id="guessInput" type="text" placeholder="输入干员名称..." autocomplete="off"/>
  <ul id="suggestions"></ul>
  <table id="guessTable">
    <thead>
      <tr>
        <th>姓名</th>
        <th>职业</th>
        <th>星级</th>
        <th>性别</th>
        <th>阵营</th>
        <th>站位</th>
        <th>实装时间</th>
        <th>对比</th>
      </tr>
    </thead>
    <tbody></tbody>
  </table>
`;

const input = document.getElementById("guessInput");
const suggestions = document.getElementById("suggestions");

input.addEventListener("input", async () => {
  const query = input.value;
  if (!query) return suggestions.innerHTML = "";
  const res = await fetch(`${API_BASE}/suggest?query=${encodeURIComponent(query)}`, {
    method: "GET",
    credentials: "include"  // ✅ 也加上
  });
  
  const names = await res.json();
  suggestions.innerHTML = names.map(name => `<li>${name}</li>`).join("");
});

suggestions.addEventListener("click", async (e) => {
  if (e.target.tagName === "LI") {
    const name = e.target.innerText;
    console.log("🧪 name =", name);
    console.log("🧪 encoded =", encodeURIComponent(name));
    input.value = "";
    suggestions.innerHTML = "";

    const res = await fetch(`${API_BASE}/guess?name=${encodeURIComponent(name)}`, {
      method: "POST",
      credentials: "include"  // ✅ 同上
    });
    
    const data = await res.json();

    console.log("🧪 guess 接口返回 =", data);

    if (data.message) {
      alert(data.message);
      return;
    }

    const row = document.createElement("tr");
    row.className = data.correct ? "correct" : "incorrect";

    const g = data.guess;
    const cmp = data.comparison;

    row.innerHTML = `
      <td>${g.name}</td>
      <td>${g.role}</td>
      <td>${g.rarity}</td>
      <td>${g.gender}</td>
      <td>${g.faction}</td>
      <td>${g.position}</td>
      <td>${g.release.split("T")[0]}</td>
      <td>
        ${Object.entries(cmp).map(([k, v]) => `<div><strong>${k}</strong>: ${v}</div>`).join("")}
      </td>
    `;

    document.querySelector("#guessTable tbody").appendChild(row);

    if (data.correct) {
      alert("恭喜你，猜对了！");
    }
  }
});
}

document.getElementById("startBtn").addEventListener("click", async () => {
const res = await fetch(`${API_BASE}/start`, {
  method: "GET",
  credentials: "include"  // ✅ 保留 Cookie（JSESSIONID）
});
const data = await res.json();
alert(data.message + " - " + data.note);
createGameUI();
});