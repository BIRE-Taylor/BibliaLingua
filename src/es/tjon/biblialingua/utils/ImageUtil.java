package es.tjon.biblialingua.utils;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

public class ImageUtil {
    private static final String HTTP = "http";
    private static final String IMAGE_AMP_2X = "@2x";
    private static final String IMAGE_SIZE = "cover-80x105{0}";
    public static final String LARGE_SIZE = "cover-140x191";
    public static final String MEDIUM_SIZE = "cover-90x122";
    public static final String SMALL_SIZE = "cover-80x105";
    private static String baseImageUrl = "http://broadcast3.lds.org/crowdsource/Mobile/GospelStudy/production/v1";

    private ImageUtil() {
    }

    public static String getBaseImageUrl() {
        return baseImageUrl;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static String getImageUrl(String string, String string2) {
        String string3 = null;
        if (string == null) return string3;
        if (string.startsWith(HTTP)) {
            return string.replace("{0}", string2);
        }
        boolean bl = string.isEmpty();
        string3 = null;
        if (bl) return string3;
        return ImageUtil.getBaseImageUrl() + "/" + string.replace(IMAGE_SIZE, (CharSequence)string2);
    }

    public static void setBaseImageUrl(String string) {
        if (string != null) {
            baseImageUrl = string;
        }
    }

    /*
     * Enabled aggressive block sorting
     */
    public static void unbindDrawables(View view) {
        if (view != null) {
            Drawable drawable;
            if (view.getBackground() != null) {
                view.getBackground().setCallback(null);
            } else if (view instanceof ImageView && (drawable = ((ImageView)view).getDrawable()) != null) {
                drawable.setCallback(null);
            }
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup)view).getChildCount(); ++i) {
                    ImageUtil.unbindDrawables(((ViewGroup)view).getChildAt(i));
                }
                if (!(view instanceof AdapterView)) {
                    ((ViewGroup)view).removeAllViews();
                }
            }
        }
    }
}
 
