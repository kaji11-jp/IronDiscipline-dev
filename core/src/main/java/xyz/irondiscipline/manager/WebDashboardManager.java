package xyz.irondiscipline.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.model.Rank;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * 軽量 Web ダッシュボード
 * JDK 組み込みの HttpServer を使用し、追加ライブラリ不要。
 * 起動時にランダムなアクセストークンを生成してコンソールに表示する。
 */
public class WebDashboardManager {

    private final IronDiscipline plugin;
    private HttpServer server;
    private final String accessToken;

    public WebDashboardManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.accessToken = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * ダッシュボードを開始
     */
    public boolean start() {
        int port = plugin.getConfig().getInt("dashboard.port", 8585);
        String bind = plugin.getConfig().getString("dashboard.bind", "0.0.0.0");

        try {
            server = HttpServer.create(new InetSocketAddress(bind, port), 0);
            server.setExecutor(Executors.newFixedThreadPool(2));

            // ---- Routes ----
            server.createContext("/", this::handleIndex);
            server.createContext("/api/status", this::handleApiStatus);
            server.createContext("/api/settings", this::handleApiSettings);
            server.createContext("/api/settings/update", this::handleApiSettingsUpdate);
            server.createContext("/api/rankroles", this::handleApiRankRoles);
            server.createContext("/api/rankroles/update", this::handleApiRankRolesUpdate);

            server.start();

            plugin.getLogger().info("========================================");
            plugin.getLogger().info("  Web Dashboard started");
            plugin.getLogger().info("  URL:   http://" + (bind.equals("0.0.0.0") ? "localhost" : bind) + ":" + port);
            plugin.getLogger().info("  Token: " + accessToken);
            plugin.getLogger().info("========================================");
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Web Dashboard failed to start", e);
            return false;
        }
    }

    /**
     * ダッシュボードを停止
     */
    public void shutdown() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web Dashboard stopped.");
        }
    }

    // ===== Auth helper =====

    private boolean authenticate(HttpExchange ex) {
        // Cookie or query param: ?token=xxx
        String query = ex.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && kv[0].equals("token") && kv[1].equals(accessToken)) {
                    return true;
                }
            }
        }
        // Authorization header
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.equals("Bearer " + accessToken)) {
            return true;
        }
        return false;
    }

    private void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendHtml(HttpExchange ex, int code, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * JSON 文字列内の特殊文字をエスケープする
     */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ===== Route handlers =====

    private void handleIndex(HttpExchange ex) throws IOException {
        if (!authenticate(ex)) {
            sendHtml(ex, 401, "<h1>401 Unauthorized</h1><p>Append ?token=YOUR_TOKEN to the URL.</p>");
            return;
        }
        sendHtml(ex, 200, buildDashboardHtml());
    }

    /**
     * GET /api/status - サーバー状態
     */
    private void handleApiStatus(HttpExchange ex) throws IOException {
        if (!authenticate(ex)) { sendJson(ex, 401, "{\"error\":\"unauthorized\"}"); return; }
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, "{\"error\":\"method not allowed\"}"); return; }

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        int linked = plugin.getLinkManager() != null ? plugin.getLinkManager().getLinkCount() : 0;
        boolean discordUp = plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled();

        String json = "{\"online\":" + online
                + ",\"max\":" + max
                + ",\"linked\":" + linked
                + ",\"discordConnected\":" + discordUp
                + ",\"version\":\"" + esc(plugin.getDescription().getVersion()) + "\""
                + "}";
        sendJson(ex, 200, json);
    }

    /**
     * GET /api/settings - 現在の Discord 設定一覧
     */
    private void handleApiSettings(HttpExchange ex) throws IOException {
        if (!authenticate(ex)) { sendJson(ex, 401, "{\"error\":\"unauthorized\"}"); return; }
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, "{\"error\":\"method not allowed\"}"); return; }

        ConfigManager cm = plugin.getConfigManager();
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"enabled\":").append(cm.isDiscordEnabled());
        sb.append(",\"bot_token\":\"").append(maskToken(cm.getDiscordBotToken())).append("\"");
        sb.append(",\"notification_channel_id\":\"").append(esc(cm.getDiscordNotificationChannel())).append("\"");
        sb.append(",\"guild_id\":\"").append(esc(cm.getDiscordGuildId())).append("\"");
        sb.append(",\"unverified_role_id\":\"").append(esc(cm.getDiscordUnverifiedRoleId())).append("\"");
        sb.append(",\"verified_role_id\":\"").append(esc(cm.getDiscordVerifiedRoleId())).append("\"");
        sb.append(",\"notification_role_id\":\"").append(esc(cm.getDiscordNotificationRoleId())).append("\"");
        sb.append(",\"console_role_id\":\"").append(esc(cm.getDiscordConsoleRoleId())).append("\"");
        sb.append(",\"admin_role_id\":\"").append(esc(cm.getDiscordAdminRoleId())).append("\"");
        sb.append("}");
        sendJson(ex, 200, sb.toString());
    }

    /**
     * POST /api/settings/update - 設定を更新
     * Body: key=value 形式 (x-www-form-urlencoded)
     */
    private void handleApiSettingsUpdate(HttpExchange ex) throws IOException {
        if (!authenticate(ex)) { sendJson(ex, 401, "{\"error\":\"unauthorized\"}"); return; }
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, "{\"error\":\"method not allowed\"}"); return; }

        Map<String, String> params = parseForm(readBody(ex));
        String key = params.get("key");
        String value = params.get("value");

        if (key == null || key.isEmpty()) {
            sendJson(ex, 400, "{\"error\":\"key is required\"}");
            return;
        }

        // 書き込み可能なキー制限
        switch (key) {
            case "notification_channel_id", "guild_id",
                 "unverified_role_id", "verified_role_id",
                 "notification_role_id", "console_role_id",
                 "admin_role_id" -> {
                plugin.getConfigManager().setDiscordSetting(key, value != null ? value : "");
                plugin.getConfigManager().reload();
                sendJson(ex, 200, "{\"ok\":true,\"key\":\"" + esc(key) + "\",\"value\":\"" + esc(value) + "\"}");
            }
            case "enabled" -> {
                boolean enabled = "true".equalsIgnoreCase(value);
                plugin.getConfigManager().setDiscordSetting("enabled", enabled);
                plugin.getConfigManager().reload();
                sendJson(ex, 200, "{\"ok\":true,\"key\":\"enabled\",\"value\":" + enabled + "}");
            }
            case "bot_token" -> {
                plugin.getConfigManager().setDiscordSetting("bot_token", value != null ? value : "");
                plugin.getConfigManager().reload();
                sendJson(ex, 200, "{\"ok\":true,\"key\":\"bot_token\",\"value\":\"(updated)\"}");
            }
            default -> sendJson(ex, 400, "{\"error\":\"unknown key: " + esc(key) + "\"}");
        }
    }

    /**
     * GET /api/rankroles - 階級ロールマッピング一覧
     */
    private void handleApiRankRoles(HttpExchange ex) throws IOException {
        if (!authenticate(ex)) { sendJson(ex, 401, "{\"error\":\"unauthorized\"}"); return; }
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, "{\"error\":\"method not allowed\"}"); return; }

        ConfigManager cm = plugin.getConfigManager();
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Rank r : Rank.values()) {
            if (!first) sb.append(",");
            first = false;
            String roleId = cm.getDiscordRankRoleId(r.name());
            sb.append("\"").append(r.name()).append("\":\"").append(esc(roleId)).append("\"");
        }
        sb.append("}");
        sendJson(ex, 200, sb.toString());
    }

    /**
     * POST /api/rankroles/update - 階級ロールを更新
     * Body: rank=RANK_NAME&role_id=123456
     */
    private void handleApiRankRolesUpdate(HttpExchange ex) throws IOException {
        if (!authenticate(ex)) { sendJson(ex, 401, "{\"error\":\"unauthorized\"}"); return; }
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, "{\"error\":\"method not allowed\"}"); return; }

        Map<String, String> params = parseForm(readBody(ex));
        String rank = params.get("rank");
        String roleId = params.get("role_id");

        if (rank == null || rank.isEmpty()) {
            sendJson(ex, 400, "{\"error\":\"rank is required\"}");
            return;
        }

        // Rank 名の検証
        try {
            Rank.valueOf(rank.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, "{\"error\":\"invalid rank: " + esc(rank) + "\"}");
            return;
        }

        plugin.getConfigManager().setDiscordRankRole(rank, roleId != null ? roleId : "");
        plugin.getConfigManager().reload();
        sendJson(ex, 200, "{\"ok\":true,\"rank\":\"" + esc(rank) + "\",\"role_id\":\"" + esc(roleId) + "\"}");
    }

    // ===== Utility =====

    private String maskToken(String token) {
        if (token == null || token.length() < 8) return "****";
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> map = new LinkedHashMap<>();
        if (body == null || body.isEmpty()) return map;
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            String k = java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(k, v);
        }
        return map;
    }

    // ===== Dashboard HTML =====

    private String buildDashboardHtml() {
        // Rank enum values for JS
        StringBuilder rankOpts = new StringBuilder("[");
        boolean first = true;
        for (Rank r : Rank.values()) {
            if (!first) rankOpts.append(",");
            first = false;
            rankOpts.append("\"").append(r.name()).append("\"");
        }
        rankOpts.append("]");

        return """
<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>IronDiscipline Dashboard</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Segoe UI',system-ui,-apple-system,sans-serif;background:#0f1117;color:#e1e4e8;min-height:100vh}
.header{background:linear-gradient(135deg,#1a1d27 0%,#2d1b3d 100%);padding:20px 32px;border-bottom:1px solid #30363d;display:flex;align-items:center;gap:16px}
.header h1{font-size:1.4rem;font-weight:600;color:#f0f6fc}
.header .badge{background:#238636;color:#fff;font-size:.7rem;padding:2px 8px;border-radius:12px;font-weight:500}
.header .badge.off{background:#da3633}
.container{max-width:1000px;margin:0 auto;padding:24px 16px}
.card{background:#161b22;border:1px solid #30363d;border-radius:12px;margin-bottom:20px;overflow:hidden}
.card-header{padding:16px 20px;border-bottom:1px solid #30363d;display:flex;align-items:center;justify-content:space-between}
.card-header h2{font-size:1rem;font-weight:600;color:#f0f6fc}
.card-body{padding:20px}
.status-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px}
.stat{background:#21262d;border-radius:8px;padding:16px;text-align:center}
.stat .num{font-size:1.8rem;font-weight:700;color:#58a6ff}
.stat .label{font-size:.75rem;color:#8b949e;margin-top:4px;text-transform:uppercase;letter-spacing:.5px}
.form-group{margin-bottom:16px}
.form-group label{display:block;font-size:.8rem;color:#8b949e;margin-bottom:6px;font-weight:500}
.form-group input,.form-group select{width:100%;background:#0d1117;border:1px solid #30363d;color:#e1e4e8;padding:10px 12px;border-radius:8px;font-size:.9rem;transition:border-color .2s}
.form-group input:focus,.form-group select:focus{outline:none;border-color:#58a6ff;box-shadow:0 0 0 3px rgba(88,166,255,.15)}
.form-row{display:grid;grid-template-columns:1fr 1fr;gap:16px}
@media(max-width:600px){.form-row{grid-template-columns:1fr}}
.btn{display:inline-flex;align-items:center;gap:6px;padding:8px 20px;border:none;border-radius:8px;font-size:.85rem;font-weight:500;cursor:pointer;transition:all .15s}
.btn-primary{background:#238636;color:#fff}
.btn-primary:hover{background:#2ea043}
.btn-sm{padding:6px 14px;font-size:.8rem}
.btn-danger{background:#da3633;color:#fff}
.btn-danger:hover{background:#f85149}
.toast{position:fixed;top:20px;right:20px;background:#238636;color:#fff;padding:12px 20px;border-radius:8px;font-size:.85rem;z-index:9999;opacity:0;transform:translateY(-10px);transition:all .3s;pointer-events:none}
.toast.show{opacity:1;transform:translateY(0)}
.toast.error{background:#da3633}
table{width:100%;border-collapse:collapse}
table th,table td{padding:10px 12px;text-align:left;border-bottom:1px solid #21262d;font-size:.85rem}
table th{color:#8b949e;font-weight:500;font-size:.75rem;text-transform:uppercase;letter-spacing:.5px}
table td input{background:#0d1117;border:1px solid #30363d;color:#e1e4e8;padding:6px 10px;border-radius:6px;width:100%;font-size:.85rem}
table td input:focus{outline:none;border-color:#58a6ff}
.rank-badge{display:inline-block;padding:2px 8px;border-radius:4px;font-size:.75rem;font-weight:600;background:#21262d;color:#e1e4e8}
.actions{display:flex;gap:8px;justify-content:flex-end;margin-top:16px}
.spinner{display:inline-block;width:16px;height:16px;border:2px solid rgba(255,255,255,.3);border-top-color:#fff;border-radius:50%;animation:spin .6s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
</style>
</head>
<body>
<div class="header">
  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#58a6ff" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
  <h1>IronDiscipline</h1>
  <span class="badge" id="discordBadge">--</span>
</div>

<div class="container">
  <!-- Status -->
  <div class="card">
    <div class="card-header"><h2>サーバー状態</h2><button class="btn btn-sm btn-primary" onclick="loadStatus()">更新</button></div>
    <div class="card-body">
      <div class="status-grid">
        <div class="stat"><div class="num" id="sOnline">--</div><div class="label">オンライン</div></div>
        <div class="stat"><div class="num" id="sMax">--</div><div class="label">最大人数</div></div>
        <div class="stat"><div class="num" id="sLinked">--</div><div class="label">連携済み</div></div>
        <div class="stat"><div class="num" id="sVersion">--</div><div class="label">バージョン</div></div>
      </div>
    </div>
  </div>

  <!-- Discord Settings -->
  <div class="card">
    <div class="card-header"><h2>Discord 設定</h2></div>
    <div class="card-body">
      <div class="form-group">
        <label>Discord 連携</label>
        <select id="cfgEnabled"><option value="true">有効</option><option value="false">無効</option></select>
      </div>
      <div class="form-group">
        <label>Bot Token</label>
        <input id="cfgBotToken" type="password" placeholder="Bot Token を入力">
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>ギルド (サーバー) ID</label>
          <input id="cfgGuild" placeholder="例: 123456789012345678">
        </div>
        <div class="form-group">
          <label>通知チャンネル ID</label>
          <input id="cfgChannel" placeholder="例: 123456789012345678">
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>未認証ロール ID</label>
          <input id="cfgUnverified" placeholder="未認証ロール">
        </div>
        <div class="form-group">
          <label>認証済みロール ID</label>
          <input id="cfgVerified" placeholder="認証済みロール">
        </div>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>通知ロール ID</label>
          <input id="cfgNotify" placeholder="通知ロール">
        </div>
        <div class="form-group">
          <label>コンソールロール ID</label>
          <input id="cfgConsole" placeholder="コンソールロール">
        </div>
      </div>
      <div class="form-group">
        <label>管理者ロール ID</label>
        <input id="cfgAdmin" placeholder="管理者ロール">
      </div>
      <div class="actions">
        <button class="btn btn-primary" onclick="saveSettings()">設定を保存</button>
      </div>
    </div>
  </div>

  <!-- Rank Roles -->
  <div class="card">
    <div class="card-header"><h2>階級ロール マッピング</h2></div>
    <div class="card-body">
      <table>
        <thead><tr><th>階級</th><th>Discord ロール ID</th><th></th></tr></thead>
        <tbody id="rankTable"></tbody>
      </table>
    </div>
  </div>
</div>

<div class="toast" id="toast"></div>

<script>
const RANKS = """ + rankOpts.toString() + """
;
const TOKEN = new URLSearchParams(location.search).get('token') || '';
const headers = () => ({'Authorization':'Bearer '+TOKEN,'Content-Type':'application/x-www-form-urlencoded'});

function toast(msg, err){
  const t=document.getElementById('toast');
  t.textContent=msg;t.className='toast'+(err?' error':'')+' show';
  setTimeout(()=>t.className='toast',2500);
}

async function api(path,opts){
  const sep=path.includes('?')?'&':'?';
  const r=await fetch(path+sep+'token='+TOKEN,opts);
  if(!r.ok){const e=await r.json().catch(()=>({}));throw new Error(e.error||r.statusText);}
  return r.json();
}

async function loadStatus(){
  try{
    const d=await api('/api/status');
    document.getElementById('sOnline').textContent=d.online;
    document.getElementById('sMax').textContent=d.max;
    document.getElementById('sLinked').textContent=d.linked;
    document.getElementById('sVersion').textContent=d.version;
    const b=document.getElementById('discordBadge');
    b.textContent=d.discordConnected?'Discord 接続中':'Discord 未接続';
    b.className='badge'+(d.discordConnected?'':' off');
  }catch(e){toast('状態の取得に失敗: '+e.message,true);}
}

async function loadSettings(){
  try{
    const d=await api('/api/settings');
    document.getElementById('cfgEnabled').value=String(d.enabled);
    document.getElementById('cfgBotToken').placeholder=d.bot_token||'未設定';
    document.getElementById('cfgGuild').value=d.guild_id||'';
    document.getElementById('cfgChannel').value=d.notification_channel_id||'';
    document.getElementById('cfgUnverified').value=d.unverified_role_id||'';
    document.getElementById('cfgVerified').value=d.verified_role_id||'';
    document.getElementById('cfgNotify').value=d.notification_role_id||'';
    document.getElementById('cfgConsole').value=d.console_role_id||'';
    document.getElementById('cfgAdmin').value=d.admin_role_id||'';
  }catch(e){toast('設定の取得に失敗: '+e.message,true);}
}

async function saveSettings(){
  const pairs=[
    ['enabled',document.getElementById('cfgEnabled').value],
    ['guild_id',document.getElementById('cfgGuild').value],
    ['notification_channel_id',document.getElementById('cfgChannel').value],
    ['unverified_role_id',document.getElementById('cfgUnverified').value],
    ['verified_role_id',document.getElementById('cfgVerified').value],
    ['notification_role_id',document.getElementById('cfgNotify').value],
    ['console_role_id',document.getElementById('cfgConsole').value],
    ['admin_role_id',document.getElementById('cfgAdmin').value],
  ];
  const tok=document.getElementById('cfgBotToken').value;
  if(tok) pairs.push(['bot_token',tok]);
  try{
    for(const [k,v] of pairs){
      await api('/api/settings/update',{method:'POST',headers:headers(),body:'key='+encodeURIComponent(k)+'&value='+encodeURIComponent(v)});
    }
    toast('設定を保存しました');
    loadSettings();
  }catch(e){toast('保存に失敗: '+e.message,true);}
}

async function loadRankRoles(){
  try{
    const d=await api('/api/rankroles');
    const tbody=document.getElementById('rankTable');
    tbody.innerHTML='';
    for(const r of RANKS){
      const tr=document.createElement('tr');
      tr.innerHTML='<td><span class="rank-badge">'+r+'</span></td>'
        +'<td><input id="rr_'+r+'" value="'+(d[r]||'')+'" placeholder="ロール ID"></td>'
        +'<td><button class="btn btn-sm btn-primary" onclick="saveRankRole(\\''+r+'\\')">保存</button></td>';
      tbody.appendChild(tr);
    }
  }catch(e){toast('階級ロールの取得に失敗: '+e.message,true);}
}

async function saveRankRole(rank){
  const val=document.getElementById('rr_'+rank).value;
  try{
    await api('/api/rankroles/update',{method:'POST',headers:headers(),body:'rank='+rank+'&role_id='+encodeURIComponent(val)});
    toast(rank+' のロールを保存しました');
  }catch(e){toast('保存に失敗: '+e.message,true);}
}

// Init
loadStatus();loadSettings();loadRankRoles();
setInterval(loadStatus,30000);
</script>
</body>
</html>
""";
    }
}
