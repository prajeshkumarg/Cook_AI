package com.stcvit.cookai.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.stcvit.cookai.JsonPlaceholderApi;
import com.stcvit.cookai.R;
import com.stcvit.cookai.model.IngredientsPost;
import com.stcvit.cookai.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    //Variables for UI elements
    AutoCompleteTextView autoCompleteTextView_ingredients;
    ChipGroup chipGroup_ingredients;
    ArrayList<String> ingredients_list;
    ImageView image_emptyvessel,image_filledvessel;
    Button button_findrecipes;
    TextView textView_pantryStatus,privacy_policy;
    Chip chip;
    NestedScrollView nestedScrollView_main;

    //Other variables
    private Retrofit retrofit;
    String ing_temp = "";
    ArrayList<IngredientsPost> ingredientsPosts_list = new ArrayList<>();
    int count = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ingredients_list = new ArrayList<>();

        //Network Initialization
        retrofit=new Retrofit.Builder()
                .baseUrl(Utils.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        findViewById();

        //Checking the ingredients list when coming from another activity
        try{
            Bundle bundle = getIntent().getExtras();
            bundle.getString("Clear");
            {
                ingredients_list.clear();
                chipGroup_ingredients.removeAllViews();
                count = 0;
                ingredientsPosts_list.clear();
                check_vesselImage();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //Setting adapter to the AutoCompleteText
        String[] ingredients=getResources().getStringArray(R.array.ingredients_array);
        ArrayAdapter<String> adapter= new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,ingredients);
        autoCompleteTextView_ingredients.setAdapter(adapter);

        //Adding and Removing chip items from the ingredients chip group
        autoCompleteTextView_ingredients.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(inputCheck(ingredients_list,parent.getItemAtPosition(position).toString())) {
                    ingredients_list.add(parent.getItemAtPosition(position).toString());
                    Log.i("DATA",ingredients_list.toString());
                    check_vesselImage();
                    LayoutInflater layoutInflater_chips=LayoutInflater.from(MainActivity.this);
                    chip=(Chip)layoutInflater_chips.inflate(R.layout.chip_ingredient,null,false);
                    chip.setClickable(false);
                    chip.setTag(count);
                    count++;
                    chip.setText(parent.getItemAtPosition(position).toString());
                    chip.setOnCloseIconClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int i  = (int) v.getTag();
                            Chip chipt=v.findViewWithTag(i);
                            ingredients_list.remove(chipt.getText().toString());
                            Log.i("ID",ingredients_list.toString());
                            check_vesselImage();
                            chipGroup_ingredients.removeView(v);
                        }
                    });
                    chipGroup_ingredients.addView(chip);
                    autoCompleteTextView_ingredients.setText("");

                }

            }
        });

        onClickListener();

    }

    private void onClickListener() {
        privacy_policy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData((Uri
                        .parse(Utils.PRIVACY_URL)));
                startActivity(intent);
            }
        });

        button_findrecipes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                ingredients_list.clear();
                String request = "";
                for (int i = 0; i < chipGroup_ingredients.getChildCount(); i++) {
                    String text_from_chip = ((Chip) chipGroup_ingredients.getChildAt(i)).getText().toString();
                    ingredients_list.add(text_from_chip);
                    ing_temp = ing_temp + ", " + text_from_chip;
                    request = request + "," + text_from_chip;
                }
                if(sizeCheck(ingredients_list)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    View dialogview = getLayoutInflater().inflate(R.layout.loading_layout, null);
                    builder.setView(dialogview);
                    AlertDialog dialog
                            = builder.create();
                    dialog.show();
                    request = request.substring(1, request.length());
                    Log.i("REQUEST", request);

                    JsonPlaceholderApi jsonPlaceholderApi = retrofit.create(JsonPlaceholderApi.class);
                    IngredientsPost ingredientsPost = new IngredientsPost(request);

                    try {
                        Log.i("JSON req", ingredientsPost.getFoodItems());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("REQFAIL",e.toString());
                    }



                    Call<List<IngredientsPost>> call = jsonPlaceholderApi.postIngredients(ingredientsPost);
                    call.enqueue(new Callback<List<IngredientsPost>>() {
                        @Override
                        public void onResponse(Call<List<IngredientsPost>> call, Response<List<IngredientsPost>> response) {
                            if (!response.isSuccessful()) {
                                Log.i("FAILED in ", response.toString());
                                dialog.cancel();
                            } else {
                                Log.i("Success :: ", response.body().get(0).getTitle());
                                int size = response.body().size();
                                ingredientsPosts_list.clear();
                                for (int i = 0; i < size; i++) {
                                    String title = response.body().get(i).getTitle();
                                    String ingredients = response.body().get(i).getIngredients();
                                    int time = response.body().get(i).getTime();
                                    String imgurl = response.body().get(i).getImgurl();
                                    String cuisine = response.body().get(i).getCuisine();
                                    String instruction = response.body().get(i).getInstructions();
                                    ingredientsPosts_list.add(new IngredientsPost(title, ingredients, time, imgurl, cuisine, instruction));
                                }
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("Recipes", (Serializable) ingredientsPosts_list);
                                Intent intent = new Intent(MainActivity.this, RecipesActivity.class);
                                intent.putExtras(bundle);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_in_right,
                                        R.anim.slide_out_left);
                                dialog.cancel();
                            }
                        }

                        @SuppressLint("NewApi")
                        @Override
                        public void onFailure(Call<List<IngredientsPost>> call, Throwable t) {
                            Log.i("FAILURE out ", t.toString());
                            Snackbar.make(nestedScrollView_main, "We are experiencing some error, Please retry! ", Snackbar.LENGTH_SHORT)
                                    .setBackgroundTint(getApplicationContext()
                                            .getColor(R.color.textColor)).setTextColor(getColor(R.color.white)).show();
                            dialog.cancel();
                        }
                    });
                }
            }
        });
    }

    private void findViewById() {
        nestedScrollView_main=findViewById(R.id.main_activity_layout);
        privacy_policy= findViewById(R.id.tv_privacy);
        autoCompleteTextView_ingredients=findViewById(R.id.textfield_input);
        chipGroup_ingredients=findViewById(R.id.ingredient_chips);
        textView_pantryStatus=findViewById(R.id.text_pantrystatus);
        button_findrecipes=findViewById(R.id.button_findrecipes);
        image_emptyvessel=findViewById(R.id.imageview_emptyvessel);
        image_emptyvessel.setVisibility(View.VISIBLE);
        image_filledvessel=findViewById(R.id.imageview_filledvessel);
    }

    private boolean inputCheck(ArrayList<String> ingredients_list, String str) {
        int size = ingredients_list.size();
        if(!ingredients_list.contains(str)){
            return true;
        }
        else {
            Snackbar.make(nestedScrollView_main,"You have already added that ingredient", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getApplicationContext()
                            .getColor(R.color.textColor)).setTextColor(getColor(R.color.white)).show();
            return false;}
    }



    private boolean sizeCheck(ArrayList<String> stringArrayList){
        if(stringArrayList.size()>=4){
            return true;
        }
        else {
            Snackbar.make(nestedScrollView_main,"Minimum 4 ingredients required", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getApplicationContext()
                            .getColor(R.color.textColor)).setTextColor(getColor(R.color.white)).show();
            return false;}
    }

    private void check_vesselImage(){
        if(ingredients_list.isEmpty()){
            image_emptyvessel.setVisibility(View.VISIBLE);
            textView_pantryStatus.setVisibility(View.VISIBLE);
            image_filledvessel.setVisibility(View.INVISIBLE);
            button_findrecipes.setVisibility(View.INVISIBLE);
        }
        else{
            image_emptyvessel.setVisibility(View.INVISIBLE);
            textView_pantryStatus.setVisibility(View.INVISIBLE);
            image_filledvessel.setVisibility(View.VISIBLE);
            button_findrecipes.setVisibility(View.VISIBLE);
        }
    }
}