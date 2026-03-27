package com.example.demo.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 日志存储到本地文件的工具类（单例模式）
 */
public class LogToFileUtil {
    private static final String TAG = "LogToFileUtil";
    // 日志文件前缀
    private static final String LOG_FILE_PREFIX = "app_log_";
    // 日志文件后缀
    private static final String LOG_FILE_SUFFIX = ".txt";
    // 单个日志文件最大大小（5MB）
    private static final long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024;
    // 日期格式化（按天分割文件）
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    // 日志内容格式化
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA);

    // 单例实例
    private static volatile LogToFileUtil INSTANCE;
    // 线程池（用于异步写入日志）
    private final ExecutorService mExecutorService;
    // 同步锁（保证多线程写入安全）
    private final Lock mLock = new ReentrantLock();
    // 上下文
    private Context mContext;

    private LogToFileUtil(Context context) {
        this.mContext = context.getApplicationContext();
        // 初始化单线程池，避免多线程写入冲突
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 获取单例实例
     */
    public static LogToFileUtil getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LogToFileUtil.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LogToFileUtil(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 写入日志（对外暴露的核心方法）
     *
     * @param tag     日志标签
     * @param content 日志内容
     * @param level   日志级别（d/i/w/e）
     */
    public void writeLog(String tag, String content, String level) {
        // 异步写入，避免阻塞主线程
        mExecutorService.execute(() -> {
            mLock.lock(); // 加锁保证线程安全
            try {
                File logFile = getLogFile();
                if (logFile == null) {
                    Log.e(TAG, "日志文件创建失败");
                    return;
                }

                // 检查文件大小，超过限制则新建文件（追加后缀）
                if (logFile.length() > MAX_LOG_FILE_SIZE) {
                    String newFileName = LOG_FILE_PREFIX + DATE_FORMAT.format(new Date()) + "_" + System.currentTimeMillis() + LOG_FILE_SUFFIX;
                    logFile = new File(logFile.getParent(), newFileName);
                }

                // 构造日志内容
                String logContent = String.format(
                        Locale.CHINA,
                        "[%s] [%s] [%s] %s\n",
                        TIME_FORMAT.format(new Date()),
                        level.toUpperCase(),
                        tag,
                        content
                );

                // 写入文件（追加模式）
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
                writer.write(logContent);
                writer.flush();
                writer.close();

                // 同时打印到Logcat，方便调试
                printToLogcat(tag, content, level);

            } catch (IOException e) {
                Log.e(TAG, "写入日志文件失败：" + e.getMessage());
            } finally {
                mLock.unlock(); // 解锁
            }
        });
    }

    /**
     * 获取日志文件（优先应用私有目录，无需动态权限）
     */
    private File getLogFile() {
        File logDir;
        logDir = new File(mContext.getExternalFilesDir(null), "logs");

        // 创建目录（不存在则创建）
        if (!logDir.exists() && !logDir.mkdirs()) {
            Log.e(TAG, "日志目录创建失败：" + logDir.getAbsolutePath());
            return null;
        }

        // 按天命名日志文件
        String fileName = LOG_FILE_PREFIX + DATE_FORMAT.format(new Date()) + LOG_FILE_SUFFIX;
        return new File(logDir, fileName);
    }

    /**
     * 打印到Logcat
     */
    private void printToLogcat(String tag, String content, String level) {
        switch (level.toLowerCase()) {
            case "d": // Debug
                Log.d(tag, content);
                break;
            case "i": // Info
                Log.i(tag, content);
                break;
            case "w": // Warning
                Log.w(tag, content);
                break;
            case "e": // Error
                Log.e(tag, content);
                break;
            default:
                Log.v(tag, content);
                break;
        }
    }

    /**
     * 获取日志文件存储路径（用于调试/查看）
     */
    public String getLogFilePath() {
        File logFile = getLogFile();
        return logFile != null ? logFile.getAbsolutePath() : "获取路径失败";
    }

    /**
     * 释放资源
     */
    public void release() {
        if (!mExecutorService.isShutdown()) {
            mExecutorService.shutdown();
        }
        mContext = null;
        INSTANCE = null;
    }

    // 简化调用的静态方法
    public static void d(Context context, String tag, String content) {
        getInstance(context).writeLog(tag, content, "d");
    }

    public static void i(Context context, String tag, String content) {
        getInstance(context).writeLog(tag, content, "i");
    }

    public static void w(Context context, String tag, String content) {
        getInstance(context).writeLog(tag, content, "w");
    }

    public static void e(Context context, String tag, String content) {
        getInstance(context).writeLog(tag, content, "e");
    }
}