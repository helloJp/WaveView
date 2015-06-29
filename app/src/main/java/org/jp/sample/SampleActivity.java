package org.jp.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.jp.waveview.WaveView;

public class SampleActivity extends AppCompatActivity {

    private WaveView mNormalWave;
    private WaveView mRiseUpWave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        initView();
    }

    private void initView() {
        mNormalWave = (WaveView) findViewById(R.id.normal_wave);
        mRiseUpWave = (WaveView) findViewById(R.id.rise_up_wave);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_normal:
                if (mNormalWave.getVisibility() != View.VISIBLE) {
                    mNormalWave.setVisibility(View.VISIBLE);
                    mRiseUpWave.setVisibility(View.GONE);
                }
                break;
            case R.id.action_rise_up:
                if (mRiseUpWave.getVisibility() != View.VISIBLE) {
                    mRiseUpWave.setVisibility(View.VISIBLE);
                    mNormalWave.setVisibility(View.GONE);
                    mRiseUpWave.setWaveLineHeight(300);
                }
                break;
            case R.id.action_percent_text:
                mRiseUpWave.setShowPercentText(!mRiseUpWave.getIsShowPercentText());
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
