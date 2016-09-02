import java.util.ArrayList;
import java.util.List;

import lrscp.lib.Log;

public class LogFilter {
    private static final String TAG = LogFilter.class.getSimpleName();

    private String mLogLevel;
    private String mRegx;
    private String tag;
    private String pid;
    private List<String> keywords = new ArrayList<String>();

    public LogFilter(String logLevel, String regx) {
        mLogLevel = logLevel;
        mRegx = regx;
        String[] segs = mRegx.split(" ");
        for (String s : segs) {
            if (s.startsWith("tag:")) {
                tag = s.substring(4);
            } else if (s.startsWith("pid:")) {
                pid = s.substring(4);
            } else {
                keywords.add(s);
            }
        }
        // Log.d(TAG, "tag=" + tag);
    }

    public boolean match(LogInfo log, boolean isShowExtra) {
        return match(tag,log.content);
    }

    public static boolean match(String tag, String log) {
        try {
            if (null == tag || " ".equals(tag)) return true;
            if (null == log || log.length() == 0) return true;
            TagItem tagItem = new TagItem(null, tag, 0, null, false);
            tagItem = getNextTagItem(tagItem);
            while (null != tagItem) {
                if (tagItem.isAnd) {
                    tagItem.isMatch = tagItem.match(log) && tagItem.prev.match();
                } else {
                    tagItem.isMatch = tagItem.match(log) || tagItem.prev.match();
                }
               /* if (!tagItem.isMatch) {
                    return false;
                }*/
                TagItem nextItem = getNextTagItem(tagItem);
                if (null == nextItem) {
                    return tagItem.match();
                }else {
                    tagItem = nextItem;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static TagItem getNextTagItem(TagItem tagItem) {
        if (tagItem.index < tagItem.tag.length()) {
            int index = tagItem.index;
            String itemChars = "";
            boolean isAdd = false;
            while (index < tagItem.tag.length()) {
                char c = tagItem.tag.charAt(index);
                if (c == '|' || c == '&') {
                    if (tagItem.index == index) {
                        if (c == '&') isAdd = true;
                        index++;
                    } else {
                        break;
                    }
                } else {
                    index++;
                    itemChars += c;
                }
            }
            return new TagItem(tagItem, tagItem.tag, index, itemChars, isAdd);
        } else {
            return null;
        }
    }

    private static class TagItem {
        private String tag;
        private int index;
        private String item;
        private boolean isAnd;
        private TagItem prev;
        private boolean isMatch = false;

        public TagItem(TagItem prev, String tag, int index, String item, boolean isAnd) {
            this.prev = prev;
            this.tag = tag;
            this.index = index;
            this.item = item;
            this.isAnd = isAnd;
        }


        @Override
        public String toString() {
            return "TagItem{" +
                    "tag='" + tag + '\'' +
                    ", index=" + index +
                    ", item='" + item + '\'' +
                    ", isAnd=" + isAnd +
                    '}';
        }

        public boolean match() {
            return isMatch;
        }

        public boolean match(String content) {
            this.isMatch = content.contains(item);
            return isMatch;
        }
    }

    public static void main(String[] args) {
        System.out.println(false&false|true);
        System.out.println(match("abc", "acabc"));
       /* TagItem tagItem = new TagItem(null,"a|b&c", 0, null, false);

        tagItem = getNextTagItem(tagItem);
        while (null != tagItem) {
            System.out.println(tagItem);
            tagItem = getNextTagItem(tagItem);
        }*/
    }

    public boolean consume(LogInfo log, boolean isShowExtra) {
        try {
            if (log.tag.contains("DEBUG") || log.tag.contains("libc")) {
                return false;
            }

            if (!isShowExtra && (log.tag == null || log.tag.isEmpty())) {
                return true;
            }

            if (pid != null && !log.pid.equals(pid)) {
                return true;
            }

            if (LogInfo.compareLevel(log.logLevel, mLogLevel) < 0) {
                return true;
            }

            if (tag != null && !log.tag.toLowerCase().contains(tag.toLowerCase())) {
                return true;
            }

            for (String keyword : keywords) {
                if (!log.content.toLowerCase().contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Log.e("compare log level error:" + mLogLevel);
        }

        return false;
    }

}
