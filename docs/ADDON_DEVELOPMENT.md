# IronDiscipline アドオン開発ガイド

> **対象バージョン**: IronDiscipline API 2.0.0-dev  
> **Minecraft**: Paper / Folia 1.20.4  
> **Java**: 17+

---

## 目次

1. [はじめに](#1-はじめに)
2. [開発環境のセットアップ](#2-開発環境のセットアップ)
3. [最初のアドオンプラグイン](#3-最初のアドオンプラグイン)
4. [API アクセスパターン](#4-api-アクセスパターン)
5. [階級システム (Rank System)](#5-階級システム-rank-system)
   - [5.1 IRank インターフェース](#51-irank-インターフェース)
   - [5.2 CoreRanks — 組み込み階級定数](#52-coreranks--組み込み階級定数)
   - [5.3 RankRegistry — 階級レジストリ](#53-rankregistry--階級レジストリ)
   - [5.4 カスタム階級の登録](#54-カスタム階級の登録)
6. [プロバイダインターフェース](#6-プロバイダインターフェース)
   - [6.1 IRankProvider — 階級管理](#61-irankprovider--階級管理)
   - [6.2 IJailProvider — 隔離管理](#62-ijailprovider--隔離管理)
   - [6.3 IDivisionProvider — 部隊管理](#63-idivisionprovider--部隊管理)
   - [6.4 IKillLogProvider — キルログ](#64-ikilllogprovider--キルログ)
   - [6.5 IStorageProvider — DB 共有](#65-istorageprovider--db-共有)
   - [6.6 IEconomyProvider — 経済 (Phase 2)](#66-ieconomyprovider--経済-phase-2)
   - [6.7 ITerritoryProvider — 領土 (Phase 2)](#67-iterritoryprovider--領土-phase-2)
7. [カスタムイベント](#7-カスタムイベント)
   - [7.1 RankChangeEvent](#71-rankchangeevent)
   - [7.2 PlayerJailEvent](#72-playerjailevent)
   - [7.3 PlayerUnjailEvent](#73-playerunjailevent)
   - [7.4 PlayerKillEvent](#74-playerkillevent)
8. [モデルクラス](#8-モデルクラス)
   - [8.1 KillLog](#81-killlog)
   - [8.2 JailRecord](#82-jailrecord)
9. [DB 共有とカスタムテーブル](#9-db-共有とカスタムテーブル)
10. [プロバイダ実装の提供 (上級)](#10-プロバイダ実装の提供-上級)
11. [Folia 互換性の注意点](#11-folia-互換性の注意点)
12. [完全なサンプルアドオン](#12-完全なサンプルアドオン)
13. [API リファレンス一覧](#13-api-リファレンス一覧)
14. [トラブルシューティング](#14-トラブルシューティング)

---

## 1. はじめに

**IronDiscipline** は、軍事 / 監獄 RP サーバー向けの Folia 対応 Minecraft プラグインです。  
バージョン 2.0 から **Maven マルチモジュール構成** を導入し、アドオンプラグインが安定した API を通じて IronDiscipline の機能を利用・拡張できるようになりました。

### アーキテクチャ概要

```
┌─────────────────────────────────────────────┐
│                 Paper / Folia Server         │
├─────────────────────────────────────────────┤
│                                             │
│  ┌──────────────┐      ┌──────────────────┐ │
│  │ IrDi-Core    │      │ Your Addon       │ │
│  │ (plugin JAR) │      │ (plugin JAR)     │ │
│  │              │      │                  │ │
│  │  implements  │◄─────│  uses API        │ │
│  │  providers   │      │  listens events  │ │
│  └──────┬───────┘      └────────┬─────────┘ │
│         │                       │           │
│         ▼                       ▼           │
│  ┌──────────────────────────────────────┐   │
│  │ IronDiscipline-API (shared library)  │   │
│  │  - IRank, RankRegistry, CoreRanks    │   │
│  │  - Provider interfaces               │   │
│  │  - Custom events                      │   │
│  │  - Model classes                      │   │
│  └──────────────────────────────────────┘   │
│                                             │
│  ┌──────────────────┐                       │
│  │ ServicesManager   │  ← Provider の登録先  │
│  └──────────────────┘                       │
└─────────────────────────────────────────────┘
```

**Core** が各プロバイダインターフェースを実装し、Bukkit の `ServicesManager` に登録します。  
アドオンは `ServicesManager` または便利クラス `IronDisciplineAPI` を通じてプロバイダを取得し、API を呼び出します。

---

## 2. 開発環境のセットアップ

### 2.1 前提条件

- **Java 17** 以上
- **Maven 3.8+** または **Gradle 8+**
- Paper / Folia 1.20.4 サーバー

### 2.2 Maven でのセットアップ

#### リポジトリの追加

IronDiscipline API は Maven Central には公開されていないため、ローカルインストールまたはプライベートリポジトリを使用します。

```bash
# IronDiscipline リポジトリをクローンしてローカルにインストール
git clone https://github.com/your-org/IronDiscipline-dev.git
cd IronDiscipline-dev
mvn clean install -DskipTests
```

#### pom.xml の設定

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>irdi-my-addon</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <irondiscipline.version>2.0.0-dev</irondiscipline.version>
    </properties>

    <repositories>
        <!-- Paper API リポジトリ -->
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- Paper API -->
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.20.4-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>

        <!-- IronDiscipline API -->
        <dependency>
            <groupId>xyz.irondiscipline</groupId>
            <artifactId>IronDiscipline-API</artifactId>
            <version>${irondiscipline.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

> **重要**: `scope` は必ず `provided` にしてください。API ライブラリは Core プラグインに含まれており、サーバー上で自動的に利用可能になります。

### 2.3 Gradle でのセットアップ

```kotlin
// build.gradle.kts
plugins {
    java
}

repositories {
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("xyz.irondiscipline:IronDiscipline-API:2.0.0-dev")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

### 2.4 plugin.yml の設定

```yaml
name: MyIrDiAddon
version: '1.0.0'
main: com.example.myaddon.MyAddonPlugin
api-version: '1.20'
folia-supported: true

# IronDiscipline Core をロード前に先に読み込む
depend:
  - IronDisciplineDev

# 任意依存 (IronDiscipline がなくても動く場合)
# softdepend:
#   - IronDisciplineDev
```

> **注意**: `depend` に指定するプラグイン名は `IronDisciplineDev` です（Core の plugin.yml の `name` フィールド）。

---

## 3. 最初のアドオンプラグイン

```java
package com.example.myaddon;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.irondiscipline.api.IronDisciplineAPI;
import xyz.irondiscipline.api.provider.IRankProvider;

public class MyAddonPlugin extends JavaPlugin {

    private IRankProvider rankProvider;

    @Override
    public void onEnable() {
        // API からプロバイダを取得
        rankProvider = IronDisciplineAPI.getRankProvider();

        if (rankProvider == null) {
            getLogger().severe("IronDiscipline が見つかりません！プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("IronDiscipline API に接続しました！");

        // イベントリスナーを登録
        getServer().getPluginManager().registerEvents(
            new MyRankListener(rankProvider), this
        );
    }
}
```

---

## 4. API アクセスパターン

### 4.1 IronDisciplineAPI クラス (推奨)

`IronDisciplineAPI` は Bukkit `ServicesManager` のラッパーで、最も簡潔にプロバイダを取得できます。

```java
import xyz.irondiscipline.api.IronDisciplineAPI;

// 各プロバイダの取得
IRankProvider     ranks     = IronDisciplineAPI.getRankProvider();
IJailProvider     jail      = IronDisciplineAPI.getJailProvider();
IDivisionProvider divisions = IronDisciplineAPI.getDivisionProvider();
IKillLogProvider  killLogs  = IronDisciplineAPI.getKillLogProvider();
IStorageProvider  storage   = IronDisciplineAPI.getStorageProvider();

// Phase 2 プロバイダ (アドオンが実装・登録した場合のみ利用可能)
IEconomyProvider   economy   = IronDisciplineAPI.getEconomyProvider();
ITerritoryProvider territory = IronDisciplineAPI.getTerritoryProvider();
```

> プロバイダが登録されていない場合は `null` が返ります。必ず null チェックを行ってください。

### 4.2 ServicesManager を直接使用

Bukkit 標準の `ServicesManager` を直接使うこともできます。

```java
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicesManager;

ServicesManager sm = Bukkit.getServicesManager();
IRankProvider ranks = sm.load(IRankProvider.class);
```

### 4.3 どちらを使うべきか？

| 方式 | メリット | デメリット |
|------|---------|-----------|
| `IronDisciplineAPI` | 簡潔、インポート1行 | 内部で static Bukkit 呼び出し |
| `ServicesManager` 直接 | Bukkit 標準パターン | 記述がやや冗長 |

通常は **`IronDisciplineAPI`** の使用を推奨します。

---

## 5. 階級システム (Rank System)

IronDiscipline の階級システムは 3 つのコンポーネントで構成されています。

### 5.1 IRank インターフェース

`IRank` は階級を表す基本インターフェースです。全ての階級（組み込み・カスタム問わず）はこのインターフェースを実装します。

```java
package xyz.irondiscipline.api.rank;

public interface IRank {
    /** 階級の一意 ID (例: "SERGEANT") */
    String getId();

    /** 色コード付き表示名の原文 (例: "&e軍曹") */
    String getDisplayRaw();

    /** ChatColor で変換済みの表示名 (デフォルト実装あり) */
    default String getDisplay() {
        return ChatColor.translateAlternateColorCodes('&', getDisplayRaw());
    }

    /** 階級の重み (大きいほど上位) */
    int getWeight();

    /** この階級が other より上位かどうか */
    default boolean isHigherThan(IRank other) {
        return this.getWeight() > other.getWeight();
    }

    /** この階級が other より下位かどうか */
    default boolean isLowerThan(IRank other) {
        return this.getWeight() < other.getWeight();
    }

    /** 名前空間 (Core = "core", アドオン = 独自の値) */
    default String getNamespace() {
        return "core";
    }
}
```

#### 重要なルール

- **`getId()`** はグローバルに一意でなければなりません
- **`getWeight()`** は階級の序列を決定します。Core 階級は 10〜100 の範囲を使用しています
- **`getNamespace()`** をオーバーライドして、アドオン独自の名前空間を設定してください

### 5.2 CoreRanks — 組み込み階級定数

Core が提供する 9 つの標準階級は `CoreRanks` クラスに定数として定義されています。

```java
import xyz.irondiscipline.api.rank.CoreRanks;

IRank privateRank  = CoreRanks.PRIVATE;            // weight=10, 二等兵
IRank pfc          = CoreRanks.PRIVATE_FIRST_CLASS; // weight=15, 一等兵
IRank corporal     = CoreRanks.CORPORAL;            // weight=20, 伍長
IRank sergeant     = CoreRanks.SERGEANT;            // weight=30, 軍曹
IRank lieutenant   = CoreRanks.LIEUTENANT;          // weight=40, 少尉
IRank captain      = CoreRanks.CAPTAIN;             // weight=50, 大尉
IRank major        = CoreRanks.MAJOR;               // weight=60, 少佐
IRank colonel      = CoreRanks.COLONEL;             // weight=70, 大佐
IRank commander    = CoreRanks.COMMANDER;           // weight=100, 司令官
```

#### 全 Core 階級の一覧取得

```java
List<IRank> allCoreRanks = CoreRanks.values();
// [PRIVATE(10), PRIVATE_FIRST_CLASS(15), CORPORAL(20), ..., COMMANDER(100)]
```

#### Core 階級の詳細一覧

| 定数名 | ID | 表示名 | Weight |
|--------|----|--------|--------|
| `PRIVATE` | `PRIVATE` | `&7二等兵` | 10 |
| `PRIVATE_FIRST_CLASS` | `PRIVATE_FIRST_CLASS` | `&7一等兵` | 15 |
| `CORPORAL` | `CORPORAL` | `&a伍長` | 20 |
| `SERGEANT` | `SERGEANT` | `&e軍曹` | 30 |
| `LIEUTENANT` | `LIEUTENANT` | `&6少尉` | 40 |
| `CAPTAIN` | `CAPTAIN` | `&c大尉` | 50 |
| `MAJOR` | `MAJOR` | `&5少佐` | 60 |
| `COLONEL` | `COLONEL` | `&d大佐` | 70 |
| `COMMANDER` | `COMMANDER` | `&4司令官` | 100 |

### 5.3 RankRegistry — 階級レジストリ

`RankRegistry` は全ての階級（Core + アドオン）を管理するスレッドセーフな中央レジストリです。  
Core 起動時に自動的に 9 つの Core 階級が登録されます。

```java
import xyz.irondiscipline.api.rank.RankRegistry;

// ID から階級を取得 (見つからない場合は PRIVATE にフォールバック)
IRank rank = RankRegistry.fromId("SERGEANT");

// ID から階級を取得 (見つからない場合は null)
IRank rankOrNull = RankRegistry.fromIdOrNull("CUSTOM_RANK");

// weight から最も近い階級を取得
IRank closestRank = RankRegistry.fromWeight(35); // → SERGEANT (weight=30)

// 次の階級を取得 (昇進先)
IRank next = RankRegistry.getNextRank(CoreRanks.SERGEANT);   // → LIEUTENANT
IRank last = RankRegistry.getNextRank(CoreRanks.COMMANDER);  // → null (最高位)

// 前の階級を取得 (降格先)
IRank prev = RankRegistry.getPreviousRank(CoreRanks.SERGEANT);   // → CORPORAL
IRank first = RankRegistry.getPreviousRank(CoreRanks.PRIVATE);   // → null (最低位)

// 全階級を weight 昇順で取得
List<IRank> allRanks = RankRegistry.values();

// 名前空間でフィルタリング
List<IRank> coreOnly  = RankRegistry.valuesByNamespace("core");
List<IRank> addonOnly = RankRegistry.valuesByNamespace("my_addon");

// 登録状態の確認
boolean exists = RankRegistry.isRegistered("SERGEANT");  // true
int total = RankRegistry.size();                          // 9 (Core のみの場合)
```

### 5.4 カスタム階級の登録

アドオンプラグインは独自の階級を `RankRegistry` に登録できます。  
これにより、昇進・降格の階層に自然に組み込まれます。

#### カスタム階級の定義

```java
package com.example.myaddon.rank;

import org.bukkit.ChatColor;
import xyz.irondiscipline.api.rank.IRank;

public enum MyAddonRanks implements IRank {
    // Core の COLONEL(70) と COMMANDER(100) の間に新階級を挿入
    BRIGADIER("BRIGADIER", "&6准将", 80),
    GENERAL("GENERAL", "&4大将", 90);

    private final String id;
    private final String displayRaw;
    private final int weight;

    MyAddonRanks(String id, String displayRaw, int weight) {
        this.id = id;
        this.displayRaw = displayRaw;
        this.weight = weight;
    }

    @Override public String getId()         { return id; }
    @Override public String getDisplayRaw() { return displayRaw; }
    @Override public int    getWeight()     { return weight; }

    @Override
    public String getNamespace() {
        return "my_addon";  // "core" 以外のユニークな名前空間
    }
}
```

#### onEnable での登録

```java
@Override
public void onEnable() {
    // カスタム階級を RankRegistry に登録
    RankRegistry.registerAll(MyAddonRanks.values());
    getLogger().info("カスタム階級を " + MyAddonRanks.values().length + " 件登録しました");

    // 登録後の昇進階層:
    // PRIVATE(10) → PFC(15) → CORPORAL(20) → SERGEANT(30)
    // → LIEUTENANT(40) → CAPTAIN(50) → MAJOR(60) → COLONEL(70)
    // → BRIGADIER(80) → GENERAL(90) → COMMANDER(100)
}
```

#### onDisable での登録解除

```java
@Override
public void onDisable() {
    for (MyAddonRanks rank : MyAddonRanks.values()) {
        RankRegistry.unregister(rank.getId());
    }
}
```

#### Weight の選び方

| Core 階級間 | 空き Weight 範囲 | 推奨用途 |
|------------|-----------------|---------|
| COLONEL(70) ↔ COMMANDER(100) | 71 〜 99 | 上級将校の追加 |
| MAJOR(60) ↔ COLONEL(70) | 61 〜 69 | 中級将校の補完 |
| その他 | 各間に 5〜10 の空き | 細かなランク追加 |

> **注意**: 同じ weight を持つ階級を登録しないでください。`RankRegistry` は weight の昇順でソートするため、同 weight の階級があると順序が不定になります。

---

## 6. プロバイダインターフェース

### 6.1 IRankProvider — 階級管理

プレイヤーの階級取得・設定・昇降格を提供します。Core の `RankManager` が実装します。

#### 取得方法

```java
IRankProvider ranks = IronDisciplineAPI.getRankProvider();
```

#### メソッド一覧

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `getRank(Player)` | `IRank` | オンラインプレイヤーの階級を同期取得（キャッシュから） |
| `getRankAsync(UUID)` | `CompletableFuture<IRank>` | UUID で非同期取得（オフライン対応） |
| `setRank(Player, IRank)` | `CompletableFuture<Boolean>` | 階級を設定（Tab/ネームタグも更新） |
| `setRankByUUID(UUID, String, IRank)` | `CompletableFuture<Boolean>` | UUID で設定（オフライン対応） |
| `promote(Player)` | `CompletableFuture<IRank>` | 昇進（最高位なら null） |
| `demote(Player)` | `CompletableFuture<IRank>` | 降格（最低位なら null） |
| `requiresPTS(Player)` | `boolean` | PTS（発言許可）が必要か |
| `isHigherRank(Player, Player)` | `boolean` | officer が target より上位か |

#### 使用例

```java
IRankProvider ranks = IronDisciplineAPI.getRankProvider();

// ① 現在の階級を取得
IRank currentRank = ranks.getRank(player);
player.sendMessage("あなたの階級: " + currentRank.getDisplay());

// ② 階級比較
if (ranks.isHigherRank(officer, target)) {
    // officer は target より上位
}

// ③ 非同期で階級を設定
ranks.setRank(player, CoreRanks.CAPTAIN).thenAccept(success -> {
    if (success) {
        getLogger().info(player.getName() + " を大尉に任命しました");
    }
});

// ④ 昇進
ranks.promote(player).thenAccept(newRank -> {
    if (newRank != null) {
        player.sendMessage("昇進しました！新階級: " + newRank.getDisplay());
    } else {
        player.sendMessage("既に最高階級です");
    }
});

// ⑤ オフラインプレイヤーの階級取得
UUID offlineId = UUID.fromString("...");
ranks.getRankAsync(offlineId).thenAccept(rank -> {
    getLogger().info("オフラインプレイヤーの階級: " + rank.getId());
});
```

### 6.2 IJailProvider — 隔離管理

プレイヤーの隔離（Jail）と釈放を管理します。Core の `JailManager` が実装します。

#### 取得方法

```java
IJailProvider jail = IronDisciplineAPI.getJailProvider();
```

#### メソッド一覧

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `isJailed(UUID)` | `boolean` | 隔離中かどうか（同期） |
| `isJailedAsync(UUID)` | `CompletableFuture<Boolean>` | 隔離中かどうか（非同期） |
| `jail(Player, Player, String)` | `boolean` | プレイヤーを隔離 |
| `unjail(Player)` | `boolean` | プレイヤーを釈放 |

#### 使用例

```java
IJailProvider jail = IronDisciplineAPI.getJailProvider();

// 隔離状態の確認
if (jail.isJailed(player.getUniqueId())) {
    player.sendMessage("あなたは現在隔離中です");
    return;
}

// プレイヤーを隔離 (jailer = 実施者, null = システム)
boolean success = jail.jail(target, officer, "命令違反");

// 非同期でオフラインプレイヤーの隔離状態を確認
jail.isJailedAsync(offlineUuid).thenAccept(jailed -> {
    if (jailed) {
        getLogger().info("このプレイヤーは隔離中です");
    }
});
```

### 6.3 IDivisionProvider — 部隊管理

プレイヤーの部隊所属を管理します。Core の `DivisionManager` が実装します。

#### 取得方法

```java
IDivisionProvider divisions = IronDisciplineAPI.getDivisionProvider();
```

#### メソッド一覧

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `getDivision(UUID)` | `String` | 所属部隊 ID（未所属は null） |
| `getDivisionDisplay(UUID)` | `String` | 部隊表示名（未所属は空文字） |
| `setDivision(UUID, String)` | `void` | 部隊を設定 |
| `removeDivision(UUID)` | `void` | 部隊所属を解除 |
| `isMP(UUID)` | `boolean` | MP（憲兵）所属か |
| `divisionExists(String)` | `boolean` | 部隊が存在するか |
| `getAllDivisions()` | `Set<String>` | 全部隊 ID のセット |
| `getDivisionMembers(String)` | `Set<UUID>` | 部隊メンバーの UUID セット |

#### 使用例

```java
IDivisionProvider divisions = IronDisciplineAPI.getDivisionProvider();

// 所属部隊の確認
String divId = divisions.getDivision(player.getUniqueId());
if (divId != null) {
    String displayName = divisions.getDivisionDisplay(player.getUniqueId());
    player.sendMessage("所属部隊: " + displayName);
}

// MP かどうかの判定
if (divisions.isMP(player.getUniqueId())) {
    player.sendMessage("あなたは憲兵です");
}

// 全部隊の一覧
Set<String> allDivs = divisions.getAllDivisions();
for (String div : allDivs) {
    Set<UUID> members = divisions.getDivisionMembers(div);
    getLogger().info(div + ": " + members.size() + " 名");
}
```

### 6.4 IKillLogProvider — キルログ

PvP キルログの保存と取得を行います。Core の `StorageManager` が実装します。

#### 取得方法

```java
IKillLogProvider killLogs = IronDisciplineAPI.getKillLogProvider();
```

#### メソッド一覧

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `saveKillLogAsync(KillLog)` | `CompletableFuture<Void>` | キルログを保存 |
| `getKillLogsAsync(UUID, int)` | `CompletableFuture<List<KillLog>>` | プレイヤーのキルログを取得 |
| `getAllKillLogsAsync(int)` | `CompletableFuture<List<KillLog>>` | 全キルログを取得 |

#### 使用例

```java
IKillLogProvider killLogs = IronDisciplineAPI.getKillLogProvider();

// プレイヤーの直近 10 件のキルログを取得
killLogs.getKillLogsAsync(player.getUniqueId(), 10).thenAccept(logs -> {
    for (KillLog log : logs) {
        getLogger().info(
            log.getKillerName() + " killed " + log.getVictimName()
            + " with " + log.getWeapon()
            + " at " + log.getFormattedDistance()
        );
    }
});

// カスタムキルログの保存
KillLog customLog = new KillLog.Builder()
    .killer(killer.getUniqueId(), killer.getName())
    .victim(victim.getUniqueId(), victim.getName())
    .weapon("Custom Weapon")
    .distance(15.5)
    .location("world", 100.0, 64.0, 200.0)
    .build();

killLogs.saveKillLogAsync(customLog);
```

### 6.5 IStorageProvider — DB 共有

Core のデータベース接続を共有し、アドオンが独自テーブルを作成・利用できるようにします。  
Core の `IronDiscipline` メインクラスが実装します。

#### 取得方法

```java
IStorageProvider storage = IronDisciplineAPI.getStorageProvider();
```

#### メソッド一覧

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `getConnection()` | `Connection` | 共有 DB 接続 |
| `getDbExecutor()` | `ExecutorService` | DB 操作用スレッドプール |
| `getDatabaseType()` | `String` | `"h2"` または `"mysql"` |

#### 使用例

詳細は [セクション 9: DB 共有とカスタムテーブル](#9-db-共有とカスタムテーブル) を参照してください。

### 6.6 IEconomyProvider — 経済 (Phase 2)

> **注意**: Phase 2 のプレースホルダーインターフェースです。Core では実装されていません。  
> 経済アドオンがこのインターフェースを実装し、ServicesManager に登録することで利用可能になります。

#### メソッド一覧

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `getBalance(UUID)` | `CompletableFuture<Double>` | 残高取得 |
| `withdraw(UUID, double)` | `CompletableFuture<Boolean>` | 引き出し |
| `deposit(UUID, double)` | `CompletableFuture<Boolean>` | 入金 |
| `transfer(UUID, UUID, double)` | `CompletableFuture<Boolean>` | 送金 |
| `has(UUID, double)` | `CompletableFuture<Boolean>` | 残高確認 |

### 6.7 ITerritoryProvider — 領土 (Phase 2)

> **注意**: Phase 2 のプレースホルダーインターフェースです。Core では実装されていません。  
> 領土アドオンがこのインターフェースを実装し、ServicesManager に登録することで利用可能になります。

#### メソッド一覧

| メソッド | 戻り値 | 説明 |
|---------|--------|------|
| `getOwner(Chunk)` | `CompletableFuture<UUID>` | チャンク所有者 |
| `claim(Player, Chunk)` | `CompletableFuture<Boolean>` | チャンク請求 |
| `unclaim(Chunk)` | `CompletableFuture<Boolean>` | 所有権放棄 |
| `getClaimCount(UUID)` | `CompletableFuture<Integer>` | 請求チャンク数 |
| `getClaimedChunks(UUID)` | `CompletableFuture<Set<Long>>` | 所有チャンクキーのセット |
| `isClaimed(Chunk)` | `CompletableFuture<Boolean>` | 請求済みか |

---

## 7. カスタムイベント

IronDiscipline は 4 つのカスタム Bukkit イベントを発火します。アドオンは `@EventHandler` で通常のイベントと同様にリスンできます。

### 7.1 RankChangeEvent

プレイヤーの階級が変更された際に発火します（昇進・降格・手動設定・自動昇進すべて）。

- **非同期**: `true`（DB 操作後に呼ばれるため）
- **キャンセル可能**: `false`

#### フィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `player` | `Player` | 階級変更されたプレイヤー |
| `oldRank` | `IRank` | 変更前の階級 |
| `newRank` | `IRank` | 変更後の階級 |
| `cause` | `Cause` | 変更原因 |

#### Cause 列挙型

| 値 | 説明 |
|----|------|
| `PROMOTE` | `/promote` コマンドによる昇進 |
| `DEMOTE` | `/demote` コマンドによる降格 |
| `SET` | `/irondev setrank` 等の手動設定 |
| `AUTO_PROMOTE` | AutoPromotionManager による自動昇進 |
| `API` | プラグイン API 経由 |
| `OTHER` | その他 |

#### 便利メソッド

```java
event.isPromotion(); // 昇進かどうか (newRank.weight > oldRank.weight)
event.isDemotion();  // 降格かどうか (newRank.weight < oldRank.weight)
```

#### リスナー例

```java
import xyz.irondiscipline.api.event.RankChangeEvent;
import xyz.irondiscipline.api.rank.CoreRanks;

@EventHandler
public void onRankChange(RankChangeEvent event) {
    Player player = event.getPlayer();
    IRank newRank = event.getNewRank();

    // 昇進時のみ処理
    if (event.isPromotion()) {
        // 大尉以上に昇進した場合、特別報酬を付与
        if (newRank.getWeight() >= CoreRanks.CAPTAIN.getWeight()) {
            player.sendMessage("§6おめでとうございます！特別報酬が付与されました！");
            // 報酬処理...
        }
    }

    // 原因ごとの処理
    switch (event.getCause()) {
        case AUTO_PROMOTE:
            getLogger().info(player.getName() + " が自動昇進しました");
            break;
        case API:
            getLogger().info("API経由で " + player.getName() + " の階級が変更されました");
            break;
    }
}
```

### 7.2 PlayerJailEvent

プレイヤーが隔離される**直前**に発火します。

- **非同期**: `false`（メインスレッドで実行）
- **キャンセル可能**: `true` — `setCancelled(true)` で隔離を阻止できます

#### フィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `player` | `Player` | 隔離されるプレイヤー |
| `reason` | `String` | 隔離理由 |
| `jailedBy` | `UUID` | 隔離実施者の UUID（システムの場合は null） |

#### リスナー例

```java
import xyz.irondiscipline.api.event.PlayerJailEvent;

@EventHandler
public void onPlayerJail(PlayerJailEvent event) {
    Player target = event.getPlayer();

    // 特定の階級以上の隔離を阻止
    IRankProvider ranks = IronDisciplineAPI.getRankProvider();
    IRank rank = ranks.getRank(target);

    if (rank.getWeight() >= CoreRanks.COLONEL.getWeight()) {
        event.setCancelled(true);
        // 隔離実施者への通知
        if (event.getJailedBy() != null) {
            Player jailer = Bukkit.getPlayer(event.getJailedBy());
            if (jailer != null) {
                jailer.sendMessage("§c大佐以上の階級のプレイヤーは隔離できません");
            }
        }
    }

    // ログ出力
    getLogger().info(target.getName() + " が隔離されます: " + event.getReason());
}
```

### 7.3 PlayerUnjailEvent

プレイヤーが釈放される**直前**に発火します。

- **非同期**: `false`
- **キャンセル可能**: `true` — `setCancelled(true)` で釈放を阻止できます

#### フィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `player` | `Player` | 釈放されるプレイヤー |

#### リスナー例

```java
import xyz.irondiscipline.api.event.PlayerUnjailEvent;

@EventHandler
public void onPlayerUnjail(PlayerUnjailEvent event) {
    Player player = event.getPlayer();

    // 特定条件で釈放を阻止
    if (shouldRemainJailed(player)) {
        event.setCancelled(true);
        player.sendMessage("§cまだ釈放条件を満たしていません");
        return;
    }

    // 釈放完了時の処理
    player.sendMessage("§a釈放されました。今後は規律を守ってください。");
}
```

### 7.4 PlayerKillEvent

PvP キルが発生し、`KillLog` が DB に保存された**後**に発火します。

- **非同期**: `true`（DB 保存後のため）
- **キャンセル可能**: `false`

#### フィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `killer` | `Player` | キルしたプレイヤー |
| `victim` | `Player` | 死亡したプレイヤー |
| `killLog` | `KillLog` | 保存されたキルログ |

#### リスナー例

```java
import xyz.irondiscipline.api.event.PlayerKillEvent;

@EventHandler
public void onPlayerKill(PlayerKillEvent event) {
    Player killer = event.getKiller();
    Player victim = event.getVictim();
    KillLog log = event.getKillLog();

    // キルスコアの集計
    addKillScore(killer.getUniqueId(), 1);

    // 長距離キルの場合はボーナス
    if (log.getDistance() > 100.0) {
        killer.sendMessage("§6ロングショット！ (" + log.getFormattedDistance() + ")");
        addKillScore(killer.getUniqueId(), 2); // ボーナスポイント
    }

    // 武器ごとの統計
    trackWeaponStats(killer.getUniqueId(), log.getWeapon());
}
```

---

## 8. モデルクラス

### 8.1 KillLog

PvP キルの詳細情報を保持する不変オブジェクトです。Builder パターンで生成します。

#### フィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `id` | `long` | DB 上のレコード ID |
| `timestamp` | `long` | キル発生時刻（Unix ミリ秒） |
| `killerId` | `UUID` | キラーの UUID |
| `killerName` | `String` | キラーの名前 |
| `victimId` | `UUID` | 被害者の UUID |
| `victimName` | `String` | 被害者の名前 |
| `weapon` | `String` | 使用武器（デフォルト: `"不明"`） |
| `distance` | `double` | キル距離（ブロック単位） |
| `world` | `String` | ワールド名 |
| `x`, `y`, `z` | `double` | キル地点の座標 |

#### Builder の使用

```java
import xyz.irondiscipline.api.model.KillLog;

KillLog log = new KillLog.Builder()
    .killer(killerUuid, "KillerName")
    .victim(victimUuid, "VictimName")
    .weapon("Diamond Sword")
    .distance(5.3)
    .location("world", 100.0, 64.0, -200.0)
    .timestamp(System.currentTimeMillis())
    .build();

// ゲッター
String formatted = log.getFormattedDistance(); // "5.3m"
```

### 8.2 JailRecord

隔離（収監）記録を保持する不変オブジェクトです。

#### フィールド

| フィールド | 型 | 説明 |
|-----------|------|------|
| `playerId` | `UUID` | 隔離されたプレイヤーの UUID |
| `playerName` | `String` | プレイヤー名 |
| `reason` | `String` | 隔離理由 |
| `jailedAt` | `long` | 隔離時刻（Unix ミリ秒） |
| `jailedBy` | `UUID` | 隔離実施者の UUID |
| `originalLocation` | `String` | 隔離前の位置情報（シリアライズ済み） |
| `inventoryBackup` | `String` | インベントリバックアップ（Base64） |
| `armorBackup` | `String` | 装備バックアップ（Base64） |

---

## 9. DB 共有とカスタムテーブル

アドオンは `IStorageProvider` を通じて Core と同一のデータベースにアクセスし、独自テーブルを作成できます。

### 9.1 基本パターン

```java
public class MyAddonStorage {

    private final IStorageProvider storageProvider;
    private final String dbType;

    public MyAddonStorage() {
        this.storageProvider = IronDisciplineAPI.getStorageProvider();
        this.dbType = storageProvider.getDatabaseType();
    }

    /**
     * テーブル初期化 — onEnable で呼び出す
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try (Statement stmt = storageProvider.getConnection().createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS addon_economy (
                        player_id VARCHAR(36) PRIMARY KEY,
                        balance DOUBLE DEFAULT 0.0,
                        last_updated BIGINT DEFAULT 0
                    )
                    """);
            } catch (SQLException e) {
                throw new RuntimeException("テーブル作成に失敗しました", e);
            }
        }, storageProvider.getDbExecutor()); // 必ず dbExecutor で実行
    }
}
```

### 9.2 H2 / MySQL 互換 SQL

Core は H2 と MySQL の両方をサポートしています。アドオンも両方に対応する必要があります。

```java
public CompletableFuture<Void> upsertBalance(UUID playerId, double balance) {
    return CompletableFuture.runAsync(() -> {
        Connection conn = storageProvider.getConnection();
        try {
            if ("h2".equals(storageProvider.getDatabaseType())) {
                // H2: MERGE INTO ... KEY (...)
                try (PreparedStatement ps = conn.prepareStatement(
                    "MERGE INTO addon_economy KEY (player_id) VALUES (?, ?, ?)"
                )) {
                    ps.setString(1, playerId.toString());
                    ps.setDouble(2, balance);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } else {
                // MySQL: INSERT ... ON DUPLICATE KEY UPDATE
                try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO addon_economy (player_id, balance, last_updated) "
                    + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE "
                    + "balance = VALUES(balance), last_updated = VALUES(last_updated)"
                )) {
                    ps.setString(1, playerId.toString());
                    ps.setDouble(2, balance);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("残高更新に失敗しました", e);
        }
    }, storageProvider.getDbExecutor());
}
```

### 9.3 重要な注意事項

1. **`getConnection()` で取得した接続を `close()` しないでください** — Core と全アドオンで共有されています
2. **全 DB 操作は `getDbExecutor()` が返す ExecutorService 上で実行してください** — 接続はシングルスレッドで管理されています
3. **テーブル名にはプレフィックスを付けてください** — 他のアドオンとの衝突を避けるため（例: `addon_economy_*`）
4. **H2 と MySQL の両方の SQL を記述してください** — `getDatabaseType()` で分岐します

---

## 10. プロバイダ実装の提供 (上級)

Phase 2 のインターフェース（`IEconomyProvider`, `ITerritoryProvider`）はアドオンが実装を提供します。

### 10.1 プロバイダの実装

```java
package com.example.economy;

import xyz.irondiscipline.api.provider.IEconomyProvider;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyManager implements IEconomyProvider {

    private final IStorageProvider storage;

    public EconomyManager(IStorageProvider storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = storage.getConnection().prepareStatement(
                "SELECT balance FROM addon_economy WHERE player_id = ?"
            )) {
                ps.setString(1, playerId.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getDouble("balance") : 0.0;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, storage.getDbExecutor());
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID playerId, double amount) {
        return getBalance(playerId).thenCompose(balance -> {
            if (balance < amount) {
                return CompletableFuture.completedFuture(false);
            }
            return updateBalance(playerId, balance - amount);
        });
    }

    @Override
    public CompletableFuture<Boolean> deposit(UUID playerId, double amount) {
        return getBalance(playerId).thenCompose(balance ->
            updateBalance(playerId, balance + amount)
        );
    }

    @Override
    public CompletableFuture<Boolean> transfer(UUID fromId, UUID toId, double amount) {
        return withdraw(fromId, amount).thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);
            return deposit(toId, amount);
        });
    }

    @Override
    public CompletableFuture<Boolean> has(UUID playerId, double amount) {
        return getBalance(playerId).thenApply(balance -> balance >= amount);
    }

    private CompletableFuture<Boolean> updateBalance(UUID playerId, double newBalance) {
        // ... DB更新処理
        return CompletableFuture.completedFuture(true);
    }
}
```

### 10.2 ServicesManager への登録

```java
@Override
public void onEnable() {
    IStorageProvider storage = IronDisciplineAPI.getStorageProvider();
    EconomyManager economy = new EconomyManager(storage);

    // ServicesManager に登録
    getServer().getServicesManager().register(
        IEconomyProvider.class,
        economy,
        this,
        ServicePriority.Normal
    );

    getLogger().info("Economy プロバイダを登録しました");
}

@Override
public void onDisable() {
    // 登録解除
    getServer().getServicesManager().unregisterAll(this);
}
```

### 10.3 他のアドオンからの利用

```java
// 別のアドオンから Economy API を利用
IEconomyProvider economy = IronDisciplineAPI.getEconomyProvider();
if (economy != null) {
    economy.getBalance(player.getUniqueId()).thenAccept(balance -> {
        player.sendMessage("残高: " + balance);
    });
}
```

---

## 11. Folia 互換性の注意点

IronDiscipline は **Folia** をネイティブサポートしています。アドオン開発時も以下のルールを守ってください。

### 11.1 Bukkit.getScheduler() は使用禁止

Folia 環境では `Bukkit.getScheduler()` は使用できません。代わりに Folia 互換のスケジューリングが必要です。

```java
// ❌ NG — Folia で動作しない
Bukkit.getScheduler().runTask(plugin, () -> {
    player.teleport(loc);
});

// ✅ OK — Paper/Folia 互換
// Folia 非対応のアドオンでは folialib 等のライブラリを使用するか、
// 自前で Folia API を使用してください
player.getScheduler().run(plugin, task -> {
    player.teleport(loc);
}, null);
```

### 11.2 プレイヤー操作はエンティティスレッドで

Folia ではプレイヤーの操作はそのエンティティが所属するリージョンスレッドで行う必要があります。

```java
// プレイヤーに関する操作
player.getScheduler().run(plugin, task -> {
    player.sendMessage("Hello!");
    player.teleport(destination);
}, null);
```

### 11.3 非同期イベントハンドラの注意

`RankChangeEvent` と `PlayerKillEvent` は `async=true` で発火されます。  
イベントハンドラ内でプレイヤーを操作する場合は、エンティティスレッドに切り替えてください。

```java
@EventHandler
public void onRankChange(RankChangeEvent event) {
    // このメソッドは非同期スレッドで実行される

    Player player = event.getPlayer();

    // ❌ 直接プレイヤーを操作すると問題が発生する可能性がある
    // player.teleport(rewardLocation);

    // ✅ エンティティスレッドに切り替える
    player.getScheduler().run(plugin, task -> {
        player.teleport(rewardLocation);
        player.sendMessage("報酬の場所にテレポートしました");
    }, null);
}
```

### 11.4 plugin.yml の設定

```yaml
folia-supported: true
```

---

## 12. 完全なサンプルアドオン

以下は、全 API 機能を活用した完全なサンプルアドオンです。

### プロジェクト構成

```
irdi-war-stats/
├── pom.xml
├── src/main/java/com/example/warstats/
│   ├── WarStatsPlugin.java
│   ├── listener/
│   │   ├── KillStatsListener.java
│   │   └── RankRewardListener.java
│   └── command/
│       └── StatsCommand.java
└── src/main/resources/
    └── plugin.yml
```

### plugin.yml

```yaml
name: IrDiWarStats
version: '1.0.0'
main: com.example.warstats.WarStatsPlugin
api-version: '1.20'
folia-supported: true
depend:
  - IronDisciplineDev
commands:
  warstats:
    description: 戦績を表示
    usage: /warstats [player]
```

### WarStatsPlugin.java

```java
package com.example.warstats;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.irondiscipline.api.IronDisciplineAPI;
import xyz.irondiscipline.api.provider.*;
import xyz.irondiscipline.api.rank.RankRegistry;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class WarStatsPlugin extends JavaPlugin {

    private IRankProvider rankProvider;
    private IKillLogProvider killLogProvider;
    private IStorageProvider storageProvider;
    private final ConcurrentHashMap<UUID, Integer> killScores = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        // プロバイダの取得
        rankProvider = IronDisciplineAPI.getRankProvider();
        killLogProvider = IronDisciplineAPI.getKillLogProvider();
        storageProvider = IronDisciplineAPI.getStorageProvider();

        if (rankProvider == null || killLogProvider == null || storageProvider == null) {
            getLogger().severe("IronDiscipline API に接続できません！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // カスタムテーブル作成
        initDatabase().thenRun(() -> {
            getLogger().info("データベース初期化完了");
        });

        // イベントリスナー登録
        getServer().getPluginManager().registerEvents(
            new KillStatsListener(this), this
        );
        getServer().getPluginManager().registerEvents(
            new RankRewardListener(this), this
        );

        // コマンド登録
        getCommand("warstats").setExecutor(new StatsCommand(this));

        getLogger().info("IrDi-WarStats が有効化されました！");
    }

    private CompletableFuture<Void> initDatabase() {
        return CompletableFuture.runAsync(() -> {
            try (Statement stmt = storageProvider.getConnection().createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS war_kill_scores (
                        player_id VARCHAR(36) PRIMARY KEY,
                        kills INT DEFAULT 0,
                        deaths INT DEFAULT 0,
                        score INT DEFAULT 0
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, storageProvider.getDbExecutor());
    }

    public IRankProvider getRankProvider() { return rankProvider; }
    public IKillLogProvider getKillLogProvider() { return killLogProvider; }
    public IStorageProvider getStorageProvider() { return storageProvider; }
    public ConcurrentHashMap<UUID, Integer> getKillScores() { return killScores; }
}
```

### KillStatsListener.java

```java
package com.example.warstats.listener;

import com.example.warstats.WarStatsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.irondiscipline.api.event.PlayerKillEvent;
import xyz.irondiscipline.api.model.KillLog;

public class KillStatsListener implements Listener {

    private final WarStatsPlugin plugin;

    public KillStatsListener(WarStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerKill(PlayerKillEvent event) {
        Player killer = event.getKiller();
        Player victim = event.getVictim();
        KillLog log = event.getKillLog();

        // キルスコアを計算
        int baseScore = 10;

        // 長距離ボーナス
        if (log.getDistance() > 50.0) {
            baseScore += 5;
            // 非同期イベントなので、メッセージ送信はスケジューリング
            killer.getScheduler().run(plugin, task -> {
                killer.sendMessage("§6◆ ロングショットボーナス！ +" + 5 + "pt (" +
                    log.getFormattedDistance() + ")");
            }, null);
        }

        // スコア更新
        plugin.getKillScores().merge(killer.getUniqueId(), baseScore, Integer::sum);

        // DB にも保存
        final int finalScore = baseScore;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try (var ps = plugin.getStorageProvider().getConnection().prepareStatement(
                "h2".equals(plugin.getStorageProvider().getDatabaseType())
                    ? "MERGE INTO war_kill_scores KEY (player_id) VALUES (?, ?, 0, ?)"
                    : "INSERT INTO war_kill_scores (player_id, kills, score) VALUES (?, 1, ?) "
                      + "ON DUPLICATE KEY UPDATE kills = kills + 1, score = score + ?"
            )) {
                ps.setString(1, killer.getUniqueId().toString());
                if ("h2".equals(plugin.getStorageProvider().getDatabaseType())) {
                    int current = plugin.getKillScores().getOrDefault(
                        killer.getUniqueId(), 0);
                    ps.setInt(2, current);
                    ps.setInt(3, current);
                } else {
                    ps.setInt(2, finalScore);
                    ps.setInt(3, finalScore);
                }
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("スコア保存失敗: " + e.getMessage());
            }
        }, plugin.getStorageProvider().getDbExecutor());
    }
}
```

### RankRewardListener.java

```java
package com.example.warstats.listener;

import com.example.warstats.WarStatsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.irondiscipline.api.event.RankChangeEvent;
import xyz.irondiscipline.api.rank.CoreRanks;
import xyz.irondiscipline.api.rank.IRank;

public class RankRewardListener implements Listener {

    private final WarStatsPlugin plugin;

    public RankRewardListener(WarStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRankChange(RankChangeEvent event) {
        // 昇進時のみ処理
        if (!event.isPromotion()) return;

        Player player = event.getPlayer();
        IRank newRank = event.getNewRank();
        IRank oldRank = event.getOldRank();

        // 非同期イベントなので、プレイヤー操作はスケジューリング
        player.getScheduler().run(plugin, task -> {
            player.sendMessage("§b═══════════════════════════════════");
            player.sendMessage("§e  ★ 昇進おめでとうございます！ ★");
            player.sendMessage("§7  " + oldRank.getDisplay() + " §7→ " + newRank.getDisplay());
            player.sendMessage("§b═══════════════════════════════════");

            // 士官以上 (weight >= 40) に昇進した場合はダイヤモンドソードを支給
            if (newRank.getWeight() >= CoreRanks.LIEUTENANT.getWeight()
                && oldRank.getWeight() < CoreRanks.LIEUTENANT.getWeight()) {
                player.sendMessage("§6士官に任命されました。指揮刀を支給します。");
                // アイテム配布処理...
            }
        }, null);

        // 変更原因のログ
        plugin.getLogger().info(
            player.getName() + " が " + event.getCause().name()
            + " により " + newRank.getId() + " に昇進"
        );
    }
}
```

### StatsCommand.java

```java
package com.example.warstats.command;

import com.example.warstats.WarStatsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.irondiscipline.api.IronDisciplineAPI;
import xyz.irondiscipline.api.model.KillLog;
import xyz.irondiscipline.api.rank.IRank;

import java.util.List;

public class StatsCommand implements CommandExecutor {

    private final WarStatsPlugin plugin;

    public StatsCommand(WarStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ実行可能です");
            return true;
        }

        // 階級情報
        IRank rank = plugin.getRankProvider().getRank(player);
        player.sendMessage("§b══ 戦績情報 ══");
        player.sendMessage("§7階級: " + rank.getDisplay());
        player.sendMessage("§7スコア: §e" +
            plugin.getKillScores().getOrDefault(player.getUniqueId(), 0) + "pt");

        // 直近5件のキルログ
        plugin.getKillLogProvider()
            .getKillLogsAsync(player.getUniqueId(), 5)
            .thenAccept(logs -> {
                // 非同期コールバック → プレイヤー操作はスケジューリング
                player.getScheduler().run(plugin, task -> {
                    player.sendMessage("§7--- 直近の戦闘記録 ---");
                    if (logs.isEmpty()) {
                        player.sendMessage("§7  記録なし");
                    } else {
                        for (KillLog log : logs) {
                            boolean isKiller = log.getKillerId().equals(player.getUniqueId());
                            String symbol = isKiller ? "§a⚔ Kill" : "§c☠ Death";
                            String opponent = isKiller ? log.getVictimName() : log.getKillerName();
                            player.sendMessage("  " + symbol + " §7vs " + opponent
                                + " §8(" + log.getWeapon() + ", " + log.getFormattedDistance() + ")");
                        }
                    }
                }, null);
            });

        return true;
    }
}
```

---

## 13. API リファレンス一覧

### パッケージ構成

| パッケージ | 内容 |
|-----------|------|
| `xyz.irondiscipline.api` | `IronDisciplineAPI` — 中央アクセスポイント |
| `xyz.irondiscipline.api.rank` | `IRank`, `CoreRanks`, `RankRegistry` |
| `xyz.irondiscipline.api.model` | `KillLog`, `JailRecord` |
| `xyz.irondiscipline.api.provider` | 7 つのプロバイダインターフェース |
| `xyz.irondiscipline.api.event` | 4 つのカスタムイベント |

### クラス/インターフェース一覧

| クラス | 種別 | 説明 |
|--------|------|------|
| `IronDisciplineAPI` | class | ServicesManager ラッパー、静的メソッドでプロバイダ取得 |
| `IRank` | interface | 階級を表すインターフェース |
| `CoreRanks` | class | 9 つの組み込み階級定数 |
| `RankRegistry` | class | スレッドセーフな階級レジストリ |
| `KillLog` | class | PvP キルログモデル（Builder あり） |
| `JailRecord` | class | 隔離記録モデル |
| `IRankProvider` | interface | 階級管理（Core 実装） |
| `IJailProvider` | interface | 隔離管理（Core 実装） |
| `IDivisionProvider` | interface | 部隊管理（Core 実装） |
| `IKillLogProvider` | interface | キルログ管理（Core 実装） |
| `IStorageProvider` | interface | DB 接続共有（Core 実装） |
| `IEconomyProvider` | interface | 経済管理（Phase 2 — アドオン実装） |
| `ITerritoryProvider` | interface | 領土管理（Phase 2 — アドオン実装） |
| `RankChangeEvent` | class | 階級変更イベント（async, non-cancellable） |
| `PlayerJailEvent` | class | 隔離イベント（sync, cancellable） |
| `PlayerUnjailEvent` | class | 釈放イベント（sync, cancellable） |
| `PlayerKillEvent` | class | PvP キルイベント（async, non-cancellable） |

---

## 14. トラブルシューティング

### Q: プロバイダが null で返ってくる

**原因**: IronDiscipline Core がまだロードされていないか、`depend` の設定が不足している。

**対策**:
- `plugin.yml` に `depend: [IronDisciplineDev]` を追加
- `onEnable()` 内でプロバイダを取得する（コンストラクタでは早すぎる場合がある）

### Q: ClassNotFoundException: xyz.irondiscipline.api.xxx

**原因**: API ライブラリがクラスパスにない。

**対策**:
- `pom.xml` に `IronDiscipline-API` 依存を追加
- `scope` は `provided` にする（JAR に含める必要はない）
- Core プラグイン JAR がサーバーの `plugins/` に配置されているか確認

### Q: RankRegistry にカスタム階級が登録されていない

**原因**: 登録タイミングの問題、または重複 ID。

**対策**:
- `onEnable()` 内で `RankRegistry.register()` を呼ぶ
- `getId()` がグローバルに一意であることを確認
- 同じ weight の階級が既に存在しないか確認

### Q: 非同期イベントハンドラでプレイヤー操作ができない

**原因**: `RankChangeEvent` や `PlayerKillEvent` は非同期スレッドで発火される。

**対策**:
- プレイヤー操作は `player.getScheduler().run()` でエンティティスレッドに切り替える
- [セクション 11: Folia 互換性の注意点](#11-folia-互換性の注意点) を参照

### Q: H2 で動くが MySQL で SQL エラーが出る

**原因**: SQL 構文の差異。

**対策**:
- `getDatabaseType()` で分岐して SQL を書き分ける
- H2: `MERGE INTO ... KEY (...)`, MySQL: `INSERT ... ON DUPLICATE KEY UPDATE`
- [セクション 9.2](#92-h2--mysql-互換-sql) を参照

### Q: テスト環境で API が取得できない

**対策**:
- テストでは `ServicesManager` をモックしてプロバイダを返すようにする
- `Mockito` の `when(Bukkit.getServicesManager().load(...)).thenReturn(...)` パターンを使用

```java
@BeforeEach
void setup() {
    ServicesManager sm = mock(ServicesManager.class);
    when(Bukkit.getServicesManager()).thenReturn(sm);
    when(sm.load(IRankProvider.class)).thenReturn(mockRankProvider);
}
```

---

## ライセンス

IronDiscipline API は IronDiscipline プロジェクトと同一のライセンスで提供されます。  
詳細は [LICENSE](../LICENSE) を参照してください。
