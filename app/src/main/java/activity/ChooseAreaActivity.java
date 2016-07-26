package activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a.myapplication.R;

import java.util.ArrayList;
import java.util.List;

import database.CoolWeatherDB;
import model.City;
import model.County;
import model.Province;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

/**
 * Created by a on 2016/7/23.
 */
public class ChooseAreaActivity extends Activity {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView title;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<>();

    //省份
    private List<Province> provinceList;
    //城市
    private List<City> cityList;
    //县
    private List<County> countyList;
    //选中省份
    private Province selectedProvince;
    //选中城市
    private City selectedCity;
    //选中县
    private County selectedCounty;
    //当前级别
    private int currentLevel;
    //是否是从天气界面返回
    private boolean isFromWeatherActivity;

    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity",false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean("city_selected",false) && !isFromWeatherActivity) {
            Intent intent = new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        listView = (ListView) findViewById(R.id.list_view);
        title = (TextView)findViewById(R.id.title_text);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }
                else if(currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
                else if(currentLevel == LEVEL_COUNTY) {
                    String countyCode = countyList.get(position).getCountyCode();
                    Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        provinceList = coolWeatherDB.loadProvince();
        if(provinceList.size() > 0) {
            dataList.clear();
            for(Province province:provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            title.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        }
        else {
            queryFromServer(null,"province");
        }
    }

    private void queryCities() {
        cityList = coolWeatherDB.loadCity(selectedProvince.getId());
        if(cityList.size() > 0) {
            Log.d("1111","query city ok");
            dataList.clear();
            for(City city:cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            title.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        }
        else {
            Log.d("1111","query city error");
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
    }

    private void queryCounties() {
        countyList = coolWeatherDB.loadCounty(selectedCity.getId());
        if(countyList.size() > 0) {
            dataList.clear();
            for(County county:countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            title.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }
        else {
            queryFromServer(selectedCity.getCityCode(),"county");
        }
    }

    private void queryFromServer(final String code,final String type) {
        String address;
        if(!TextUtils.isEmpty(code)) {
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        }
        else {
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if(type.equals("province")) {
                    result = Utility.handleProvinceResponse(coolWeatherDB,response);
                }
                else if(type.equals("city")) {
                    result = Utility.handleCityResponse(coolWeatherDB,response,selectedProvince.getId());
                }
                else if(type.equals("county")) {
                    result = Utility.handleCountyResponse(coolWeatherDB,response,selectedCity.getId());
                }
                if(result == true) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if(type.equals("province")) {
                                queryProvinces();
                            }
                            else if(type.equals("city")) {
                                queryCities();
                            }
                            else if(type.equals("county")) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressDialog() {
        if(progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载....");
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    private void closeProgressDialog() {
        if(progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public void onBackPressed() {
        if(currentLevel == LEVEL_COUNTY) {
            queryCities();
        }
        else if(currentLevel == LEVEL_CITY) {
            queryProvinces();
        }
        else if(currentLevel == LEVEL_PROVINCE) {
            if(isFromWeatherActivity) {
                Intent intent = new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }
}
