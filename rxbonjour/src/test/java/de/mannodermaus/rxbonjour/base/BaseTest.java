package de.mannodermaus.rxbonjour.base;

import android.content.Context;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.mannodermaus.rxbonjour.internal.BonjourSchedulers;
import de.mannodermaus.rxbonjour.util.TestSchedulers;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BonjourSchedulers.class})
public abstract class BaseTest {

    protected final Context context;

    protected BaseTest() {
        this.context = mock(Context.class);
    }

    @Before public final void beforeTests() throws Exception {
        mockStatic(BonjourSchedulers.class);
        given(BonjourSchedulers.backlogSchedulers()).willReturn(TestSchedulers.backlogSchedulers());
        given(BonjourSchedulers.cleanupSchedulers()).willReturn(TestSchedulers.immediateSchedulers());
        given(BonjourSchedulers.startSchedulers()).willReturn(TestSchedulers.immediateSchedulers());

        setupMocks();
    }

    protected void setupMocks() throws Exception {
    }
}
