package xyz.irondiscipline.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import xyz.irondiscipline.IronDiscipline;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * IrDi アドオンのインストール・管理を行うマネージャー。
 * <p>
 * 公認アドオンのレジストリは
 * {@code https://www.irondiscipline.xyz/addons.php?format=json&filter=released}
 * から動的に取得され、1時間キャッシュされます。
 * 新しい公認アドオンを追加する際はWebサイト側の {@code $addons} 配列にエントリを追加するだけで
 * プラグイン側のコード変更は不要です。
 * </p>
 *
 * <h3>インストール経路</h3>
 * <ol>
 *   <li><b>公認レジストリ</b> — IrDi チームが審査・認可したアドオン一覧から自動取得</li>
 *   <li><b>GitHub リポジトリ</b> — {@code owner/repo} 形式で指定し Releases から取得</li>
 *   <li><b>直接URL</b> — JAR の URL を直接指定してダウンロード（非公認も可）</li>
 * </ol>
 */
public class AddonManager {

    private final IronDiscipline plugin;
    private final File pluginsDir;

    /** 公認レジストリ API URL */
    private static final String REGISTRY_URL = "https://www.irondiscipline.xyz/addons.php?format=json&filter=released";

    /** キャッシュ有効時間（1時間） */
    private static final long CACHE_TTL_MS = 60 * 60 * 1000L;

    /** 公認アドオンキャッシュ（short_key → エントリ） */
    private final Map<String, AddonRegistryEntry> certifiedCache = new ConcurrentHashMap<>();

    /** キャッシュ最終更新時刻 */
    private volatile long cacheTimestamp = 0;

    public AddonManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.pluginsDir = plugin.getDataFolder().getParentFile(); // server/plugins/

        // 起動時にバックグラウンドでレジストリ取得
        CompletableFuture.runAsync(this::refreshRegistry);
    }

    // ========== 公認レジストリ（Web API から動的取得） ==========

    /**
     * IrDi チーム公認アドオンの一覧を返します。
     * キャッシュが期限切れの場合はバックグラウンドで更新をトリガーします。
     */
    public Map<String, AddonRegistryEntry> getCertifiedAddons() {
        if (isCacheExpired()) {
            CompletableFuture.runAsync(this::refreshRegistry);
        }
        return Collections.unmodifiableMap(certifiedCache);
    }

    /**
     * アドオン ID が公認レジストリに登録されているかを判定します。
     */
    public boolean isCertified(String addonId) {
        if (isCacheExpired()) {
            CompletableFuture.runAsync(this::refreshRegistry);
        }
        return certifiedCache.values().stream()
                .anyMatch(e -> e.id().equalsIgnoreCase(addonId));
    }

    /**
     * レジストリキャッシュを強制リフレッシュします。
     */
    public CompletableFuture<Void> forceRefreshRegistry() {
        return CompletableFuture.runAsync(this::refreshRegistry);
    }

    private boolean isCacheExpired() {
        return System.currentTimeMillis() - cacheTimestamp > CACHE_TTL_MS;
    }

    /**
     * Webサイトの JSON API から公認アドオンレジストリを取得してキャッシュを更新します。
     */
    private void refreshRegistry() {
        try {
            plugin.getLogger().info("公認アドオンレジストリを取得中...");

            HttpURLConnection conn = (HttpURLConnection) new URL(REGISTRY_URL).openConnection();
            conn.setRequestProperty("User-Agent", "IronDiscipline-AddonManager");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != 200) {
                plugin.getLogger().warning("レジストリの取得に失敗しました (HTTP " + conn.getResponseCode() + ")");
                conn.disconnect();
                return;
            }

            String json = readStream(conn.getInputStream());
            conn.disconnect();

            // "addons" 配列をパース
            Map<String, AddonRegistryEntry> newCache = parseRegistry(json);

            if (!newCache.isEmpty()) {
                certifiedCache.clear();
                certifiedCache.putAll(newCache);
                cacheTimestamp = System.currentTimeMillis();
                plugin.getLogger().info("公認レジストリ更新完了: " + newCache.size() + " 件のアドオン");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "公認レジストリの取得に失敗しました", e);
        }
    }

    /**
     * JSON レスポンスから公認アドオン一覧をパースします。
     * <pre>
     * {
     *   "addons": [
     *     { "id": "irdi-economy", "name": "IrDi-Economy", "short_key": "economy",
     *       "github": "kaji11-jp/IrDi-Economy", "description": "...", ... },
     *     ...
     *   ]
     * }
     * </pre>
     */
    private Map<String, AddonRegistryEntry> parseRegistry(String json) {
        Map<String, AddonRegistryEntry> result = new LinkedHashMap<>();

        // "addons" 配列を探す
        int addonsIdx = json.indexOf("\"addons\"");
        if (addonsIdx < 0) return result;

        // 各アドオンオブジェクト { ... } を順に抽出
        int pos = json.indexOf('[', addonsIdx);
        if (pos < 0) return result;

        while (true) {
            int objStart = json.indexOf('{', pos);
            if (objStart < 0) break;

            int objEnd = json.indexOf('}', objStart);
            if (objEnd < 0) break;

            String obj = json.substring(objStart, objEnd + 1);

            String id = extractJsonValue(obj, "id");
            String name = extractJsonValue(obj, "name");
            String shortKey = extractJsonValue(obj, "short_key");
            String github = extractJsonValue(obj, "github");
            String description = extractJsonValue(obj, "description");

            if (id != null && shortKey != null && github != null) {
                result.put(shortKey, new AddonRegistryEntry(
                        id,
                        name != null ? name : id,
                        github,
                        description != null ? description : ""
                ));
            }

            pos = objEnd + 1;

            // 配列の終端チェック
            int nextBrace = json.indexOf('{', pos);
            int arrayEnd = json.indexOf(']', pos);
            if (arrayEnd >= 0 && (nextBrace < 0 || arrayEnd < nextBrace)) break;
        }

        return result;
    }

    // ========== インストール ==========

    /**
     * 公認アドオンをIDでインストールします。
     *
     * @param addonId 公認アドオンID（例: "economy"）
     * @return 結果メッセージ
     */
    public CompletableFuture<InstallResult> installCertified(String addonId) {
        AddonRegistryEntry entry = certifiedCache.get(addonId.toLowerCase());
        if (entry == null) {
            // キャッシュにない場合、リフレッシュしてリトライ
            return CompletableFuture.supplyAsync(() -> {
                refreshRegistry();
                AddonRegistryEntry retried = certifiedCache.get(addonId.toLowerCase());
                if (retried == null) {
                    return new InstallResult(false, "不明なアドオン: " + addonId
                            + " (利用可能: " + String.join(", ", certifiedCache.keySet()) + ")");
                }
                return null; // リトライ成功 → 後続で処理
            }).thenCompose(result -> {
                if (result != null) return CompletableFuture.completedFuture(result);
                return installFromGitHub(certifiedCache.get(addonId.toLowerCase()).githubRepo());
            });
        }
        return installFromGitHub(entry.githubRepo());
    }

    /**
     * GitHub リポジトリからアドオンをインストールします。
     *
     * @param ownerRepo "owner/repo" 形式
     * @return 結果メッセージ
     */
    public CompletableFuture<InstallResult> installFromGitHub(String ownerRepo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("アドオンをダウンロード中: " + ownerRepo);

                // 1. GitHub Releases API で最新リリース情報を取得
                ReleaseAsset asset = fetchLatestReleaseAsset(ownerRepo);
                if (asset == null) {
                    return new InstallResult(false,
                            "リリースが見つかりません: " + ownerRepo
                            + " (GitHub リポジトリにリリースを作成してください)");
                }

                // 2. JAR をダウンロード（一時ファイルへ）
                Path tempFile = Files.createTempFile("irdi-addon-", ".jar");
                downloadFile(asset.downloadUrl(), tempFile);

                // 3. irdi-addon.yml 検証
                AddonDescriptor descriptor = validateAddonJar(tempFile);
                if (descriptor == null) {
                    Files.deleteIfExists(tempFile);
                    return new InstallResult(false,
                            "無効なアドオン JAR です。irdi-addon.yml が見つかりません。"
                            + "\nIrDi アドオンとして認識できません。");
                }

                // 4. 既存JARの重複チェック＆削除
                removeExistingAddon(descriptor.id());

                // 5. plugins/ にコピー
                Path destination = pluginsDir.toPath().resolve(asset.fileName());
                Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);

                plugin.getLogger().info("アドオンインストール完了: " + descriptor.name()
                        + " v" + descriptor.version());

                String certLabel = isCertified(descriptor.id()) ? "[公認] " : "[非公認] ";
                return new InstallResult(true,
                        certLabel + descriptor.name() + " v" + descriptor.version() + " をインストールしました。"
                        + "\nサーバーを再起動すると有効になります。");

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "アドオンインストール失敗: " + ownerRepo, e);
                return new InstallResult(false, "インストールに失敗しました: " + e.getMessage());
            }
        });
    }

    /**
     * URL から直接アドオン JAR をダウンロードしてインストールします。
     * 非公認アドオンもこの方法でインストール可能です。
     *
     * @param jarUrl JAR ファイルの直接 URL
     * @return 結果メッセージ
     */
    public CompletableFuture<InstallResult> installFromUrl(String jarUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("URL からアドオンをダウンロード中: " + jarUrl);

                // ファイル名を URL から抽出
                String fileName = jarUrl.substring(jarUrl.lastIndexOf('/') + 1);
                if (!fileName.toLowerCase().endsWith(".jar")) {
                    fileName = fileName + ".jar";
                }
                // クエリパラメータ除去
                if (fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf('?'));
                }

                // 1. JAR をダウンロード（一時ファイルへ）
                Path tempFile = Files.createTempFile("irdi-addon-", ".jar");
                downloadFile(jarUrl, tempFile);

                // 2. irdi-addon.yml 検証
                AddonDescriptor descriptor = validateAddonJar(tempFile);
                if (descriptor == null) {
                    Files.deleteIfExists(tempFile);
                    return new InstallResult(false,
                            "無効なアドオン JAR です。irdi-addon.yml が見つかりません。"
                            + "\nIrDi アドオンとして認識できません。");
                }

                // 3. 既存JARの重複チェック＆削除
                removeExistingAddon(descriptor.id());

                // 4. plugins/ にコピー
                Path destination = pluginsDir.toPath().resolve(fileName);
                Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);

                String certLabel = isCertified(descriptor.id()) ? "[公認] " : "[非公認] ";
                plugin.getLogger().info("アドオンインストール完了: " + certLabel
                        + descriptor.name() + " v" + descriptor.version());

                return new InstallResult(true,
                        certLabel + descriptor.name() + " v" + descriptor.version() + " をインストールしました。"
                        + "\nサーバーを再起動すると有効になります。");

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "アドオンインストール失敗(URL): " + jarUrl, e);
                return new InstallResult(false, "インストールに失敗しました: " + e.getMessage());
            }
        });
    }

    // ========== インストール済みアドオンのスキャン ==========

    /**
     * plugins/ 内の IrDi アドオンを検出して一覧を返します。
     */
    public List<AddonDescriptor> scanInstalledAddons() {
        List<AddonDescriptor> addons = new ArrayList<>();
        File[] files = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null) return addons;

        for (File jarFile : files) {
            try {
                AddonDescriptor descriptor = readDescriptor(jarFile.toPath());
                if (descriptor != null) {
                    addons.add(descriptor);
                }
            } catch (Exception ignored) {
                // IrDi アドオンでない JAR はスキップ
            }
        }

        addons.sort(Comparator.comparing(AddonDescriptor::name));
        return addons;
    }

    // ========== アンインストール ==========

    /**
     * アドオンをアンインストール（JARを削除）します。
     */
    public CompletableFuture<InstallResult> uninstall(String addonId) {
        return CompletableFuture.supplyAsync(() -> {
            boolean removed = removeExistingAddon(addonId);
            if (removed) {
                return new InstallResult(true,
                        addonId + " をアンインストールしました。"
                        + "\nサーバーを再起動すると反映されます。");
            } else {
                return new InstallResult(false,
                        "アドオン " + addonId + " が見つかりません。");
            }
        });
    }

    // ========== GitHub API ==========

    /**
     * GitHub Releases API から最新リリースの JAR アセットを取得します。
     */
    private ReleaseAsset fetchLatestReleaseAsset(String ownerRepo) throws IOException {
        String apiUrl = "https://api.github.com/repos/" + ownerRepo + "/releases/latest";
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
        conn.setRequestProperty("User-Agent", "IronDiscipline-AddonManager");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            return null;
        }

        // JSON をシンプルにパース（外部ライブラリ不要）
        String json = readStream(conn.getInputStream());
        conn.disconnect();

        // "browser_download_url" と "name" を抽出（.jar ファイルのみ）
        // 簡易パーサー: "assets" 配列内から .jar のアセットを探す
        String downloadUrl = null;
        String fileName = null;
        String tagName = extractJsonValue(json, "tag_name");

        int assetsStart = json.indexOf("\"assets\"");
        if (assetsStart < 0) return null;

        String assetsSection = json.substring(assetsStart);
        // 各アセットの browser_download_url を探す
        int searchFrom = 0;
        while (true) {
            int urlIdx = assetsSection.indexOf("\"browser_download_url\"", searchFrom);
            if (urlIdx < 0) break;

            String url = extractJsonValueAt(assetsSection, urlIdx);
            if (url != null && url.endsWith(".jar")) {
                downloadUrl = url;
                // name を逆方向に探す
                int nameIdx = assetsSection.lastIndexOf("\"name\"", urlIdx);
                if (nameIdx >= searchFrom) {
                    fileName = extractJsonValueAt(assetsSection, nameIdx);
                }
                break;
            }
            searchFrom = urlIdx + 1;
        }

        if (downloadUrl == null || fileName == null) return null;
        return new ReleaseAsset(downloadUrl, fileName, tagName);
    }

    // ========== JAR 検証 ==========

    /**
     * JAR ファイルが IrDi アドオンかどうかを検証し、デスクリプタを返します。
     *
     * @return irdi-addon.yml が存在すれば AddonDescriptor、なければ null
     */
    private AddonDescriptor validateAddonJar(Path jarPath) {
        return readDescriptor(jarPath);
    }

    /**
     * JAR 内の irdi-addon.yml を読み取ります。
     */
    private AddonDescriptor readDescriptor(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry("irdi-addon.yml");
            if (entry == null) return null;

            try (InputStream is = jar.getInputStream(entry)) {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(new InputStreamReader(is, "UTF-8"));

                return new AddonDescriptor(
                        yaml.getString("id", "unknown"),
                        yaml.getString("name", "Unknown"),
                        yaml.getString("version", "0.0.0"),
                        yaml.getString("description", ""),
                        yaml.getString("author", ""),
                        yaml.getString("api-version", ""),
                        yaml.getString("github", ""),
                        jarPath.getFileName().toString()
                );
            }
        } catch (Exception e) {
            return null;
        }
    }

    // ========== ファイル操作 ==========

    /**
     * 既存の同一アドオン JAR を削除します。
     */
    private boolean removeExistingAddon(String targetId) {
        File[] files = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null) return false;

        boolean removed = false;
        for (File file : files) {
            try {
                AddonDescriptor desc = readDescriptor(file.toPath());
                if (desc != null && desc.id().equalsIgnoreCase(targetId)) {
                    if (file.delete()) {
                        plugin.getLogger().info("旧アドオン JAR を削除: " + file.getName());
                        removed = true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return removed;
    }

    /**
     * URL からファイルをダウンロードします。
     */
    private void downloadFile(String urlStr, Path destination) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "IronDiscipline-AddonManager");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);

        // GitHub Releases はリダイレクトする場合がある
        int responseCode = conn.getResponseCode();
        if (responseCode == 302 || responseCode == 301) {
            String redirectUrl = conn.getHeaderField("Location");
            conn.disconnect();
            conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
            conn.setRequestProperty("User-Agent", "IronDiscipline-AddonManager");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
        }

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }
    }

    // ========== JSON ヘルパー（外部ライブラリなし） ==========

    private String extractJsonValue(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        return extractJsonValueAt(json, idx);
    }

    private String extractJsonValueAt(String json, int keyIdx) {
        int colon = json.indexOf(':', keyIdx);
        if (colon < 0) return null;

        int quoteStart = json.indexOf('"', colon + 1);
        if (quoteStart < 0) return null;

        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return null;

        return json.substring(quoteStart + 1, quoteEnd);
    }

    private String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            result.write(buffer, 0, len);
        }
        return result.toString("UTF-8");
    }

    // ========== レコードクラス ==========

    /** インストール結果 */
    public record InstallResult(boolean success, String message) {}

    /** アドオンデスクリプタ（irdi-addon.yml の内容） */
    public record AddonDescriptor(String id, String name, String version, String description,
                                   String author, String apiVersion, String github,
                                   String jarFileName) {}

    /** GitHub Releases のアセット情報 */
    private record ReleaseAsset(String downloadUrl, String fileName, String tagName) {}

    /** 公認レジストリのエントリ（IrDi チーム審査済み） */
    public record AddonRegistryEntry(String id, String name, String githubRepo, String description) {}
}
