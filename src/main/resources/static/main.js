const gameDiv = document.getElementById("game");
const API_BASE = "/api";

// 创建初始界面（选择稀有度 + 开始按钮）
function createStartUI() {
  gameDiv.innerHTML = `
    <label for="raritySelect">选择稀有度：</label>
    <select id="raritySelect">
      <option value="0">任意</option>
      <option value="1">1星</option>
      <option value="2">2星</option>
      <option value="3">3星</option>
      <option value="4">4星</option>
      <option value="5">5星</option>
      <option value="6">6星</option>
    </select>
    <button id="startBtn">开始猜测</button>
  `;

  document.getElementById("startBtn").addEventListener("click", async () => {
    const rarity = document.getElementById("raritySelect").value;
    const res = await fetch(`${API_BASE}/start?rarity=${rarity}`, {
      method: "GET",
      credentials: "include"
    });
    const data = await res.json();
    // alert(data.message + " - " + data.note);
    createGameUI(); // 开始后进入游戏界面
  });
}


// 创建游戏界面（输入框 + 表格）
function createGameUI() {
  gameDiv.innerHTML = `
    <button id="backBtn">返回首页</button>
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
          <th>种族</th>
          <th>实装时间</th>
        </tr>
      </thead>
      <tbody></tbody>
    </table>
  `;

  // 跳转回 index.html
  document.getElementById("backBtn").addEventListener("click", async () => {
    window.location.href = "index.html";
  });

  const input = document.getElementById("guessInput");
  const suggestions = document.getElementById("suggestions");

  input.addEventListener("input", async () => {
    const query = input.value;
    if (!query) return suggestions.innerHTML = "";
    const res = await fetch(`${API_BASE}/suggest?query=${encodeURIComponent(query)}`, {
      method: "GET",
      credentials: "include"
    });

    const names = await res.json();
    suggestions.innerHTML = names.map(name => `<li>${name}</li>`).join("");
  });

  suggestions.addEventListener("click", async (e) => {
    if (e.target.tagName === "LI") {
      const name = e.target.innerText;
      input.value = "";
      suggestions.innerHTML = "";

      const res = await fetch(`${API_BASE}/guess?name=${encodeURIComponent(name)}`, {
        method: "POST",
        credentials: "include"
      });

      const data = await res.json();

      if (data.error) {
        alert(data.error);
        return;
      }

      const row = document.createElement("tr");
      row.className = data.correct ? "correct" : "incorrect";
      
      const g = data.guess;
      const cmp = data.comparison;

      row.innerHTML = `
        ${formatCell(g.name, cmp.name)}
        ${formatCell(g.role, cmp.role)}
        ${formatCell(g.rarity, cmp.rarity)}
        ${formatCell(g.gender, cmp.gender)}
        ${formatCell(g.faction, cmp.faction)}
        ${formatCell(g.position, cmp.position)}
        ${formatCell(g.race, cmp.race)}
        ${formatCell(g.release.split("T")[0], cmp.release)}
      `;

      document.querySelector("#guessTable tbody").appendChild(row);

      if (data.correct) {
        //alert("恭喜你，猜对了！");
      }
    }
  });
}

// 根据 comparison 设置单元格背景色与内容
function formatCell(value, comparison) {
  let bgColor = "";
  let msg = "";

  switch (comparison) {
    case "equal":
      bgColor = "#c8e6c9"; // green
      msg = "✔️";
      break;
    case "different":
      bgColor = "#ffcdd2"; // red
      msg = "✖️";
      break;
    case "close":
      bgColor = "#fff9c4"; // yellow
      msg = "⚠️";
      break;
    default:
      if (/too (high|low|late|soon)/.test(comparison)) {
        bgColor = "#ffcdd2"; // red
        const map = {
          "too high": "🔽🔽",
          "too low": "🔼🔼",
          "too late": "⏪⏪",
          "too soon": "⏩⏩"
        };
        msg = map[comparison] || "⚠️";
      } else if (/close (high|low|late|soon)/.test(comparison)) {
        bgColor = "#fff9c4"; // yellow
        const map = {
          "close high": "🔽",
          "close low": "🔼",
          "close late": "⏪",
          "close soon": "⏩"
        };
        msg = map[comparison] || "⚠️";
      }
      else {
        bgColor = "#e0e0e0"; // gray fallback
        msg = "❔";
      }
  }

  return `<td style="background-color: ${bgColor}">${value}<br><small>${msg}</small></td>`;
}

// 页面加载时进入初始界面
createStartUI();
