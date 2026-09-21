package com.zai.arabickeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

/** الشاشة الرئيسية: التفعيل، الإعدادات، التجربة */
public class MainActivity extends Activity {

    private Prefs prefs;
    private TextView tvStatus;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new Prefs(this);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        Button btnEnable = (Button) findViewById(R.id.btnEnable);
        Button btnChoose = (Button) findViewById(R.id.btnChoose);
        RadioGroup rgTheme = (RadioGroup) findViewById(R.id.rgTheme);
        RadioGroup rgAccent = (RadioGroup) findViewById(R.id.rgAccent);
        RadioGroup rgHeight = (RadioGroup) findViewById(R.id.rgHeight);
        Switch swNumRow = (Switch) findViewById(R.id.swNumRow);
        Switch swAutoCap = (Switch) findViewById(R.id.swAutoCap);
        Switch swDoubleSpace = (Switch) findViewById(R.id.swDoubleSpace);
        Switch swSound = (Switch) findViewById(R.id.swSound);
        Switch swHaptic = (Switch) findViewById(R.id.swHaptic);
        EditText etTest = (EditText) findViewById(R.id.etTest);

        btnEnable.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

        btnChoose.setOnClickListener(v -> {
            if (imm != null) imm.showInputMethodPicker();
        });

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int theme;
            if (checkedId == R.id.rbLight) theme = 0;
            else if (checkedId == R.id.rbDark) theme = 1;
            else if (checkedId == R.id.rbAmoled) theme = 2;
            else theme = -1;
            prefs.setTheme(theme);
        });

        rgAccent.setOnCheckedChangeListener((group, checkedId) -> {
            int accent;
            if (checkedId == R.id.rbAccTeal) accent = 1;
            else if (checkedId == R.id.rbAccPurple) accent = 2;
            else if (checkedId == R.id.rbAccAmber) accent = 3;
            else if (checkedId == R.id.rbAccRed) accent = 4;
            else accent = 0;
            prefs.setAccent(accent);
        });

        rgHeight.setOnCheckedChangeListener((group, checkedId) -> {
            int h;
            if (checkedId == R.id.rbHSmall) h = 0;
            else if (checkedId == R.id.rbHLarge) h = 2;
            else h = 1;
            prefs.setKeyHeight(h);
        });

        swNumRow.setOnCheckedChangeListener((b, checked) -> prefs.setNumRow(checked));
        swAutoCap.setOnCheckedChangeListener((b, checked) -> prefs.setAutoCap(checked));
        swDoubleSpace.setOnCheckedChangeListener((b, checked) -> prefs.setDoubleSpace(checked));
        swSound.setOnCheckedChangeListener((b, checked) -> prefs.setSound(checked));
        swHaptic.setOnCheckedChangeListener((b, checked) -> prefs.setHaptics(checked));

        // الحالة الابتدائية للإعدادات
        switch (prefs.theme) {
            case 0: ((RadioButton) findViewById(R.id.rbLight)).setChecked(true); break;
            case 1: ((RadioButton) findViewById(R.id.rbDark)).setChecked(true); break;
            case 2: ((RadioButton) findViewById(R.id.rbAmoled)).setChecked(true); break;
            default: ((RadioButton) findViewById(R.id.rbAuto)).setChecked(true); break;
        }
        switch (prefs.accent) {
            case 1: ((RadioButton) findViewById(R.id.rbAccTeal)).setChecked(true); break;
            case 2: ((RadioButton) findViewById(R.id.rbAccPurple)).setChecked(true); break;
            case 3: ((RadioButton) findViewById(R.id.rbAccAmber)).setChecked(true); break;
            case 4: ((RadioButton) findViewById(R.id.rbAccRed)).setChecked(true); break;
            default: ((RadioButton) findViewById(R.id.rbAccBlue)).setChecked(true); break;
        }
        switch (prefs.keyHeight) {
            case 0: ((RadioButton) findViewById(R.id.rbHSmall)).setChecked(true); break;
            case 2: ((RadioButton) findViewById(R.id.rbHLarge)).setChecked(true); break;
            default: ((RadioButton) findViewById(R.id.rbHMedium)).setChecked(true); break;
        }
        swNumRow.setChecked(prefs.numRow);
        swAutoCap.setChecked(prefs.autoCap);
        swDoubleSpace.setChecked(prefs.doubleSpace);
        swSound.setChecked(prefs.sound);
        swHaptic.setChecked(prefs.haptics);
    }

    private boolean isEnabledBySystem() {
        if (imm == null) return false;
        List<InputMethodInfo> list = imm.getEnabledInputMethodList();
        for (InputMethodInfo i : list)
            if (i.getPackageName().equals(getPackageName())) return true;
        return false;
    }

    private boolean isSelected() {
        String def = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        return def != null && def.contains(getPackageName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = isEnabledBySystem();
        boolean selected = isSelected();
        if (selected) {
            tvStatus.setText(R.string.status_ready);
            tvStatus.setTextColor(0xFF1B7E3C);
        } else if (enabled) {
            tvStatus.setText(R.string.status_enabled_not_selected);
            tvStatus.setTextColor(0xFF9A6700);
        } else {
            tvStatus.setText(R.string.status_disabled);
            tvStatus.setTextColor(0xFF444B57);
        }
    }
}
