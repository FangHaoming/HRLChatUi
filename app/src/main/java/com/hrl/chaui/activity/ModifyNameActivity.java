package com.hrl.chaui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hrl.chaui.R;

public class ModifyNameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable  Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_modify_name);
        EditText Edit=findViewById(R.id.Edit);
        Button save=findViewById(R.id.save);
        TextView back_arrow=findViewById(R.id.back_arrow);
        SharedPreferences sp=getSharedPreferences("data",MODE_PRIVATE);
        SharedPreferences.Editor editor=sp.edit();
        if(!sp.getString("user_name","").equals("")){
            Edit.setText(sp.getString("user_name",""));
        }
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(ModifyNameActivity.this,ModifyActivity.class);
                if(!sp.getString("user_name","").equals(Edit.getText().toString())){
                    Bundle bundle=new Bundle();
                    bundle.putBoolean("isModify",true);
                    bundle.putString("user_name",Edit.getText().toString());
                    intent.putExtras(bundle);
                }
                editor.putString("user_name",Edit.getText().toString());
                editor.apply();
                startActivity(intent);
                finish();
            }
        });
        back_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(ModifyNameActivity.this,ModifyActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            Intent intent=new Intent(ModifyNameActivity.this,ModifyActivity.class);
            startActivity(intent);
            finish();
        }
        return true;
    }
}