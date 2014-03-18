/*
 * Copyright 2014 Element Interactive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.elements.flipanimation;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.Toast;


/**
 * A sample activity showing the current FoldingLayout work
 */
public class MainActivity extends ActionBarActivity implements View.OnClickListener, View.OnLongClickListener {

    private FoldingLayout foldView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foldView= (FoldingLayout) findViewById(R.id.foldView);
        foldView.setOnClickListener(this);
        foldView.setOnLongClickListener(this);
        foldView.setDuration(3000);
        foldView.setInterpolator(new BounceInterpolator());
    }



    @Override
    public void onClick(View v) {
        foldView.fold();
    }

    @Override
    public boolean onLongClick(View v) {
        foldView.reverseDirection();
        Toast.makeText(this,"changed!",Toast.LENGTH_LONG).show();
    return false;
    }
}
