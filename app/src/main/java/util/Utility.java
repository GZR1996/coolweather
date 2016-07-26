package util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import database.CoolWeatherDB;
import model.City;
import model.County;
import model.Province;

/**
 * Created by a on 2016/7/23.
 */
public class Utility {

    //省份
    public synchronized static boolean handleProvinceResponse(CoolWeatherDB coolWeatherDB,String response) {
        if(!TextUtils.isEmpty(response)) {
            String[] allProvinces = response.split(",");
            if (allProvinces != null && allProvinces.length > 0) {
                for(String p:allProvinces) {
                    String[] array = p.split("\\|");
                    Province province = new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);
                    coolWeatherDB.saveProvince(province);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean handleCityResponse(CoolWeatherDB coolWeatherDB,String response,int provinceId) {
        if(!TextUtils.isEmpty(response)) {
            String[] allCities = response.split(",");
            if(allCities != null && allCities.length > 0) {
                for(String p:allCities) {
                    String[] array = p.split("\\|");
                    Log.d("1111",array[0]+"|"+array[1]);
                    City city = new City();
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    city.setProvinceId(provinceId);
                    coolWeatherDB.saveCity(city);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean handleCountyResponse(CoolWeatherDB coolWeatherDB,String response,int cityId) {
        if(!TextUtils.isEmpty(response)) {
            String [] allCounties = response.split(",");
            if(allCounties != null && allCounties.length > 0) {
                for(String p:allCounties) {
                    String[] array = p.split("\\|");
                    Log.d("1111",array[0]+"|"+array[1]);
                    County county = new County();
                    county.setCountyCode(array[0]);
                    county.setCountyName(array[1]);
                    county.setCityId(cityId);
                    coolWeatherDB.saveCounty(county);
                }
                return true;
            }
        }
        return false;
    }

    public static void handleWeatherResponse(Context context,String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            Log.d("JSON",response);
            JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
            String cityName = weatherInfo.getString("city");
            String weatherCode = weatherInfo.getString("cityid");
            String temp1 = weatherInfo.getString("temp1");
            String temp2 = weatherInfo.getString("temp2");
            String weatherDesp = weatherInfo.getString("weather");
            String publishTime = weatherInfo.getString("ptime");
            Log.d("JSON",cityName+"|"+weatherCode+"|"+temp1+"|"+temp2+"|"+weatherDesp+"|"+publishTime);
            saveWeatherInfo(context,cityName,weatherCode,temp1,temp2,weatherDesp,publishTime);
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void saveWeatherInfo(Context context,String cityName,String weatherCode,String temp1,String temp2,String weatherDesp,String publishTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年m月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("city_selected",true);
        editor.putString("city_name",cityName);
        Log.d("showWeather","savecode"+weatherCode);
        editor.putString("weather_code",weatherCode);
        editor.putString("temp1",temp1);
        editor.putString("temp2",temp2);
        editor.putString("weather_desp",weatherDesp);
        editor.putString("publish_time","今天"+publishTime+"发布");
        editor.putString("current_date",simpleDateFormat.format(new Date()));
        editor.commit();
    }
}
