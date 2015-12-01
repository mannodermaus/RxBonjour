package rxbonjour.utils;

import android.content.Context;

import java.io.IOException;

public interface BonjourUtils<M> {

	M getManager(Context context) throws IOException;
}
