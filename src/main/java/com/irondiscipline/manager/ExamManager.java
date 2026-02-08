package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 試験マネージャー
 * 試験セッションとSTS(整列)機能を管理
 */
public class ExamManager implements Listener {

    private final IronDiscipline plugin;

    // クイズ試験中のプレイヤーと現在の状態
    // Map<PlayerUUID, QuizState>
    private final Map<UUID, QuizSession> quizSessions = new ConcurrentHashMap<>();

    // 簡易的なチーム管理 (ExamQuestionManager用)
    // TeamName -> Set<UUID>
    private final Map<String, Set<UUID>> examTeams = new ConcurrentHashMap<>();

    public ExamManager(IronDiscipline plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * STS (Shoulder To Shoulder) - 整列号令
     * 指定したライン上にプレイヤーを整列させる、または整列を指示する
     */
    public void startSTS(Player officer) {
        // RP的な演出として、周囲のプレイヤーにメッセージとサウンドを送信
        String message = plugin.getConfigManager().getMessage("exam_sts_header");
        String subMessage = plugin.getConfigManager().getMessage("exam_sts_message", "%officer%", officer.getName());

        officer.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(officer.getLocation()) < 50)
                .forEach(p -> {
                    p.sendMessage(message);
                    p.sendMessage(subMessage);
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                });

        // オプション: 強制テレポートによる整列 (Roblox RP風)
        // 実装が複雑になるため、今回はメッセージのみとするが、
        // 必要に応じて officerの視線方向に一列に並べるロジックを追加可能
    }

    /**
     * 試験開始メッセージ
     */
    public void startExamSession(Player instructor, Player target, String type) {
        String msg = plugin.getConfigManager().getMessage("exam_start_broadcast",
            "%instructor%", instructor.getName(),
            "%target%", target.getName(),
            "%type%", type);
        Bukkit.broadcastMessage(msg);
    }

    /**
     * 試験合格
     */
    public void passExam(Player instructor, Player target) {
        String msg = plugin.getConfigManager().getMessage("exam_pass_broadcast", "%target%", target.getName());
        Bukkit.broadcastMessage(msg);

        plugin.getRankManager().promote(target).thenAccept(newRank -> {
            if (newRank != null) {
                target.sendMessage(plugin.getConfigManager().getMessage("exam_promotion_congrats", "%rank%", newRank.getDisplay()));
            }
        });
    }

    /**
     * 試験不合格
     */
    public void failExam(Player instructor, Player target) {
        String msg = plugin.getConfigManager().getMessage("exam_fail_broadcast", "%target%", target.getName());
        Bukkit.broadcastMessage(msg);
        // 必要ならキックなどの処理
    }

    /**
     * プレイヤーが試験中（クイズ中）かどうか
     */
    public boolean isInExam(UUID playerId) {
        return quizSessions.containsKey(playerId);
    }

    /**
     * チームメンバーを取得 (ExamQuestionManager用)
     */
    public Set<UUID> getTeamMembers(String teamName) {
        String key = teamName.toLowerCase();

        // チーム登録がない場合は、すべてのオンラインプレイヤーから
        // divisionメタデータ等で判定するのが理想だが、
        // 今回はとりあえず空セットまたは登録済みセットを返す
        return examTeams.getOrDefault(key, new HashSet<>());
    }

    /**
     * 試験チームにメンバーを追加 (API)
     */
    public void addTeamMember(String teamName, UUID playerId) {
        examTeams.computeIfAbsent(teamName.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(playerId);
    }

    /**
     * 試験チームからメンバーを削除 (API)
     */
    public void removeTeamMember(String teamName, UUID playerId) {
        String key = teamName.toLowerCase();
        if (examTeams.containsKey(key)) {
            examTeams.get(key).remove(playerId);
        }
    }

    // ===== クイズ機能 =====

    // ===== クイズ機能 =====

    public void startQuiz(Player instructor, Player target) {
        if (quizSessions.containsKey(target.getUniqueId())) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("exam_already_in_session"));
            return;
        }

        // デモ用の簡単な問題リスト
        List<Question> questions = new ArrayList<>();
        questions.add(new Question(
            plugin.getConfigManager().getRawMessage("exam_demo_q1_text"),
            plugin.getConfigManager().getRawMessage("exam_demo_q1_correct"),
            Arrays.asList(plugin.getConfigManager().getRawMessage("exam_demo_q1_valid").split(","))
        ));
        questions.add(new Question(
            plugin.getConfigManager().getRawMessage("exam_demo_q2_text"),
            plugin.getConfigManager().getRawMessage("exam_demo_q2_correct"),
            Arrays.asList(plugin.getConfigManager().getRawMessage("exam_demo_q2_valid").split(","))
        ));
        questions.add(new Question(
            plugin.getConfigManager().getRawMessage("exam_demo_q3_text"),
            plugin.getConfigManager().getRawMessage("exam_demo_q3_correct"),
            Arrays.asList(plugin.getConfigManager().getRawMessage("exam_demo_q3_valid").split(","))
        ));

        QuizSession session = new QuizSession(target.getUniqueId(), questions);
        quizSessions.put(target.getUniqueId(), session);

        target.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_start_header"));
        target.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_instruction"));
        askNextQuestion(target, session);
    }

    private void askNextQuestion(Player player, QuizSession session) {
        Question q = session.getCurrentQuestion();
        if (q == null) {
            finishQuiz(player, session);
            return;
        }
        player.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_question_format",
            "%index%", String.valueOf(session.currentIndex + 1),
            "%question%", q.text));
    }

    private void finishQuiz(Player player, QuizSession session) {
        quizSessions.remove(player.getUniqueId());

        int score = session.score;
        int total = session.questions.size();
        double percentage = (double) score / total;

        player.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_finished"));
        player.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_score", "%score%", String.valueOf(score), "%total%", String.valueOf(total)));

        if (percentage >= 0.8) { // 80%以上で合格
            Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("exam_quiz_pass_broadcast", "%player%", player.getName()));
            plugin.getRankManager().promote(player);
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_fail_message"));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!quizSessions.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true); // チャットをキャンセル
        QuizSession session = quizSessions.get(player.getUniqueId());
        String answer = event.getMessage();

        // 強制終了コマンド
        if (answer.equalsIgnoreCase("cancel")) {
            quizSessions.remove(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_cancelled"));
            return;
        }

        Question q = session.getCurrentQuestion();
        if (q != null) {
            boolean isCorrect = q.isCorrect(answer);
            if (isCorrect) {
                player.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_correct"));
                session.score++;
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("exam_quiz_incorrect", "%answer%", q.correctDisplay));
            }

            session.currentIndex++;

            // 次の問題へ（少し遅延させると親切だが、今回は即時）
            Bukkit.getScheduler().runTask(plugin, () -> askNextQuestion(player, session));
        }
    }

    // 内部クラス
    private static class QuizSession {
        List<Question> questions;
        int currentIndex = 0;
        int score = 0;

        QuizSession(UUID playerId, List<Question> questions) {
            this.questions = questions;
        }

        Question getCurrentQuestion() {
            if (currentIndex < questions.size()) {
                return questions.get(currentIndex);
            }
            return null;
        }
    }

    private static class Question {
        String text;
        String correctDisplay;
        List<String> validAnswers;

        Question(String text, String correctDisplay, List<String> validAnswers) {
            this.text = text;
            this.correctDisplay = correctDisplay;
            this.validAnswers = validAnswers;
        }

        boolean isCorrect(String answer) {
            for (String valid : validAnswers) {
                if (valid.equalsIgnoreCase(answer))
                    return true;
            }
            return false;
        }
    }
}
