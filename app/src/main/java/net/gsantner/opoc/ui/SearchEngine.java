package net.gsantner.opoc.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;

import net.gsantner.opoc.util.Callback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")

public class SearchEngine {
    public static boolean isSearchExecuting = false;
    public static Activity activity;
    public final static Pattern[] defaultIgnoredDirs = {
            Pattern.compile("^\\.git$"),
            Pattern.compile(".*[Tt]humb.*")
    };
    public final static Pattern[] defaultIgnoredFiles = {

    };

    public static class Config {
        private final List<Pattern> _ignoredRegexDirs;
        private final List<String> _ignoredExactDirs;
        private final List<Pattern> _ignoredRegexFiles;
        private final List<String> _ignoredExactFiles;

        public final boolean _isRegexQuery;
        public final boolean _isCaseSensitiveQuery;
        public final String _query;
        public final File _rootSearchDir;
        public final boolean _isSearchInContent;
        public final boolean _isShowResultOnCancel;
        public final Integer _maxSearchDepth;
        public final List<String> _ignoredDirectories;
        public final List<String> _ignoredFiles;

        public Config(final File rootSearchDir, String query, final boolean isShowResultOnCancel,
                      final Integer maxSearchDepth, final List<String> ignoredDirectories, final List<String> ignoredFiles,
                      final boolean isRegexQuery, final boolean isCaseSensitiveQuery, final boolean isSearchInContent) {

            _rootSearchDir = rootSearchDir;
            _isSearchInContent = isSearchInContent;
            _isShowResultOnCancel = isShowResultOnCancel;
            _maxSearchDepth = maxSearchDepth;
            _ignoredDirectories = ignoredDirectories;
            _ignoredFiles = ignoredFiles;
            _isRegexQuery = isRegexQuery;
            _isCaseSensitiveQuery = isCaseSensitiveQuery;

            _ignoredExactDirs = new ArrayList<String>();
            _ignoredRegexDirs = new ArrayList<Pattern>();
            splitRegexExactFiles(_ignoredDirectories, _ignoredExactDirs, _ignoredRegexDirs);

            _ignoredExactFiles = new ArrayList<String>();
            _ignoredRegexFiles = new ArrayList<Pattern>();
            splitRegexExactFiles(_ignoredFiles, _ignoredExactFiles, _ignoredRegexFiles);


            query = isRegexQuery ? query.replaceAll("(?<![.])[*]", ".*") : query;
            _query = _isCaseSensitiveQuery ? query : query.toLowerCase();
        }

        public Config(final File rootSearchDir, final String query, final boolean isShowResultOnCancel, final Integer maxSearchDepth, final List<String> ignoredDirectories, final List<String> ignoredFiles, final FileSearchDialog.Options.SearchConfigOptions configOptions) {
            this(rootSearchDir, query, isShowResultOnCancel, maxSearchDepth, ignoredDirectories, ignoredFiles, configOptions.isRegexQuery, configOptions.isCaseSensitiveQuery, configOptions.isSearchInContent);
        }


        private void splitRegexExactFiles(List<String> list, List<String> exactList, List<Pattern> regexList) {
            for (int i = 0; i < list.size(); i++) {
                String pattern = list.get(i);
                if (pattern.isEmpty()) {
                    continue;
                }

                if (pattern.startsWith("\"")) {
                    pattern = pattern.replace("\"", "");
                    if (pattern.isEmpty()) {
                        continue;
                    }
                    exactList.add(pattern);
                } else {
                    pattern = pattern.replaceAll("(?<![.])[*]", ".*");
                    regexList.add(Pattern.compile(pattern));
                }
            }
        }
    }


    public static SearchEngine.QueueSearchFilesTask queueFileSearch(Activity activity, SearchEngine.Config config, Callback.a1<List<String>> callback) {
        SearchEngine.activity = activity;
        SearchEngine.isSearchExecuting = true;
        SearchEngine.QueueSearchFilesTask task = new SearchEngine.QueueSearchFilesTask(config, callback);
        task.execute();

        return task;
    }


    public static class QueueSearchFilesTask extends AsyncTask<Void, Integer, List<String>> {
        private SearchEngine.Config _config;
        private final Callback.a1<List<String>> _callback;
        private final Pattern _regex;

        private Snackbar _snackBar;
        private Integer _countCheckedFiles;
        private Integer _currentQueueLength;
        private boolean _isCanceled;
        private Integer _currentSearchDepth;
        private List<String> _result;

        public QueueSearchFilesTask(final SearchEngine.Config config, final Callback.a1<List<String>> callback) {
            _config = config;
            _callback = callback;
            _regex = _config._isRegexQuery ? Pattern.compile(_config._query) : null;

            _countCheckedFiles = 0;
            _isCanceled = false;
            _currentSearchDepth = 0;
            _currentQueueLength = 1;

            _result = new ArrayList<>();
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bindSnackBar(_config._query);
        }


        public void bindSnackBar(String text) {
            if (!SearchEngine.isSearchExecuting) {
                return;
            }

            try {
                View view = SearchEngine.activity.findViewById(android.R.id.content);
                _snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (SearchEngine.isSearchExecuting) {
                                    bindSnackBar(text);
                                }
                            }
                        });
                _snackBar.setAction(android.R.string.cancel, (v) -> {
                    _snackBar.dismiss();
                    preCancel();
                });
                _snackBar.show();
            } catch (Exception ignored) {
            }
        }

        @Override
        protected List<String> doInBackground(Void... voidp) {
            Queue<File> mainQueue = new LinkedList<File>();
            mainQueue.add(_config._rootSearchDir);

            while (!mainQueue.isEmpty() && !isCancelled() && !_isCanceled) {
                File currentDirectory = mainQueue.remove();

                if (!currentDirectory.canRead() || currentDirectory.isFile()) {
                    continue;
                }

                _currentSearchDepth = getDirectoryDepth(_config._rootSearchDir, currentDirectory);
                if (_currentSearchDepth > _config._maxSearchDepth) {
                    break;
                }
                _currentQueueLength = mainQueue.size() + 1;
                publishProgress(_currentQueueLength, _currentSearchDepth, _result.size(), _countCheckedFiles);

                mainQueue.addAll(currentDirectoryHandler(currentDirectory));
            }

            if (_isCanceled && _result.size() == 0) {
                cancel(true);
            }

            return _result;
        }


        private Queue<File> currentDirectoryHandler(File currentDir) {
            Queue<File> subQueue = new LinkedList<File>();

            try {
                if (!currentDir.canRead() || currentDir.isFile()) {
                    return subQueue;
                }

                File[] subDirsOrFiles = currentDir.listFiles();
                for (int i = 0; i < subDirsOrFiles.length && !isCancelled() && !_isCanceled; i++) {
                    _countCheckedFiles++;
                    File subDirOrFile = subDirsOrFiles[i];

                    if (!subDirOrFile.canRead()) {
                        continue;
                    }

                    if (subDirOrFile.isDirectory()) {
                        if (isFolderIgnored(subDirOrFile) || isFileContainSymbolicLinks(subDirOrFile, currentDir)) {
                            continue;
                        }

                        subQueue.add(subDirOrFile);
                    } else {
                        if (isFileIgnored(subDirOrFile)) {
                            continue;
                        }

                        if (_config._isSearchInContent && isFileContainSearchQuery(subDirOrFile)) {
                            String path = subDirOrFile.getCanonicalPath().replace(_config._rootSearchDir.getCanonicalPath() + "/", "");
                            _result.add(path);
                        }
                    }

                    getFileIfNameMatches(subDirOrFile);

                    publishProgress(_currentQueueLength + subQueue.size(), _currentSearchDepth, _result.size(), _countCheckedFiles);
                }
            } catch (Exception ignored) {
            }

            return subQueue;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            Integer queueLength = values[0];
            Integer queueDepth = values[1];
            Integer filesFound = values[2];
            Integer countCheckedFiles = values[3];
            String snackBarText = "f:" + filesFound + " qu:" + queueLength + "|" + queueDepth + " c:" + countCheckedFiles + "\n" + _config._query;
            if (_snackBar != null) {
                _snackBar.setText(snackBarText);
            }
        }


        @Override
        protected void onPostExecute(List<String> ret) {
            super.onPostExecute(ret);
            if (_snackBar != null) {
                _snackBar.dismiss();
            }
            if (_callback != null) {
                try {
                    _callback.callback(ret);
                } catch (Exception ignored) {
                }
            }
        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
            SearchEngine.isSearchExecuting = false;
        }


        private boolean isFileContainSymbolicLinks(File file, File expectedParentDir) {
            try {
                File realParentDir = file.getCanonicalFile().getParentFile();
                if (realParentDir != null && expectedParentDir.getCanonicalPath().equals(realParentDir.getCanonicalPath())) {
                    return false;
                }
            } catch (Exception ignored) {
            }

            return true;
        }


        private void getFileIfNameMatches(File file) {
            try {
                String fileName = _config._isCaseSensitiveQuery ? file.getName() : file.getName().toLowerCase();
                boolean isMatch = _config._isRegexQuery ? _regex.matcher(fileName).matches() : fileName.contains(_config._query);

                if (isMatch) {
                    String path = file.getCanonicalPath().replace(_config._rootSearchDir.getCanonicalPath() + "/", "");
                    _result.add(path);
                }
            } catch (Exception ignored) {
            }
        }


        private Integer getDirectoryDepth(File parentDir, File childDir) {
            try {
                String parentPath = parentDir.getCanonicalPath();
                String childPath = childDir.getCanonicalPath();
                if (!childPath.startsWith(parentPath)) {
                    return -1;
                }

                String res = childPath.replace(parentPath, "");
                return res.split("/").length;
            } catch (Exception ignored) {
            }

            return -1;
        }


        private void preCancel() {
            if (_config._isShowResultOnCancel) {
                _isCanceled = true;
                return;
            }

            cancel(true);
        }


        private boolean isFileContainSearchQuery(File file) {
            boolean ret = false;

            if (!file.canRead() || file.isDirectory()) {
                return ret;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                for (String line; (line = br.readLine()) != null; ) {
                    if (isCancelled() || _isCanceled) {
                        break;
                    }

                    line = _config._isCaseSensitiveQuery ? line : line.toLowerCase();
                    boolean isMatch = _config._isRegexQuery ? _regex.matcher(line).matches() : line.contains(_config._query);
                    if (isMatch) {
                        ret = true;
                        break;
                    }
                }
            } catch (Exception ignored) {
            }

            return ret;
        }


        private boolean isFolderIgnored(File directory) {
            String dirName = directory.getName();

            for (Pattern pattern : SearchEngine.defaultIgnoredDirs) {
                if (pattern.matcher(dirName).matches()) {
                    return true;
                }
            }

            for (String pattern : _config._ignoredExactDirs) {
                if (dirName.equals(pattern)) {
                    return true;
                }
            }

            for (Pattern pattern : _config._ignoredRegexDirs) {
                if (pattern.matcher(dirName).matches()) {
                    return true;
                }
            }

            return false;
        }


        private boolean isFileIgnored(File file) {
            String fileName = file.getName();

            for (Pattern pattern : SearchEngine.defaultIgnoredFiles) {
                if (pattern.matcher(fileName).matches()) {
                    return true;
                }
            }

            for (String pattern : _config._ignoredExactFiles) {
                if (fileName.equals(pattern)) {
                    return true;
                }
            }

            for (Pattern pattern : _config._ignoredRegexFiles) {
                if (pattern.matcher(fileName).matches()) {
                    return true;
                }
            }

            return false;
        }

    }


}
