package cordova.plugins.baidu;

import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.baidu.location.Poi;

/**
 * 百度云推送插件
 *
 * @author sky
 *
 */
public class BaiduLocation extends CordovaPlugin {

	/** LOG TAG */
	private static final String LOG_TAG = BaiduLocation.class.getSimpleName();

	/** JS回调接口对象 */
	public static CallbackContext cbCtx = null;

	/** 百度定位客户端 */
	public LocationClient mLocationClient = null;

	/** 百度定位监听 */
	public BDLocationListener myListener = new BDLocationListener() {
		@Override
		public void onReceiveLocation(BDLocation location) {
			try {
				JSONObject json = new JSONObject();
				json.put("time", location.getTime());// 时间
				json.put("locType", location.getLocType());// 定位类型
				json.put("latitude", location.getLatitude());// 纬度
				json.put("longitude", location.getLongitude());// 经度
				json.put("radius", location.getRadius());// 半径
				json.put("locTypeDescription", location.getLocTypeDescription());// *****对应的定位类型说明*****
				json.put("countryCode", location.getCountryCode());// 国家码
				json.put("country", location.getCountry());// 国家名称
				json.put("cityCode", location.getCityCode());// 城市
				json.put("city", location.getCity());// 城市名称
				json.put("district", location.getDistrict());// 区
				json.put("street", location.getStreet());// 街道
				json.put("addr", location.getAddrStr());// 地址信息
				json.put("userIndoorState", location.getUserIndoorState());// *****返回用户室内外判断结果*****
				// json.put("direction", location.getDirection());//方向,不是所有的设备有
				json.put("locationDescribe", location.getLocationDescribe());// 位置语义化信息
				List<Poi> pois=location.getPoiList();
				for (Poi poi : pois) {
					JSONObject temp=new JSONObject();
					temp.put("id", poi.getId());
					temp.put("name", poi.getName());//一般是建筑名称
					temp.put("rank", poi.getRank());//
					json.accumulate("pois", temp);
				}

				if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
					json.put("speed", location.getSpeed());// 速度 单位：km/h
					json.put("satelliteNumber", location.getSatelliteNumber());// 卫星数目
					json.put("height", location.getAltitude()); // 海拔高度 单位：米
					json.put("direction", location.getDirection());// 方向
					json.put("status", location.getGpsAccuracyStatus());// *****gps质量判断*****
					json.put("describe", "gps定位成功");// 定位类型描述
				} else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
					// 运营商信息
					if (location.hasAltitude()) {// *****如果有海拔高度*****
						json.put("height", location.getAltitude());// 单位：米
					}
					json.put("operationers", location.getOperators());// 运营商信息
					json.put("describe", "网络定位成功");
				} else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
					json.put("describe", "离线定位成功，离线定位结果也是有效的");
				} else if (location.getLocType() == BDLocation.TypeServerError) {
					json.put("describe", "服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
				} else if (location.getLocType() == BDLocation.TypeNetWorkException) {
					json.put("describe", "网络不同导致定位失败，请检查网络是否通畅");
				} else if (location.getLocType() == BDLocation.TypeCriteriaException) {
					json.put("describe", "无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
				}
				PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, json);
				pluginResult.setKeepCallback(true);
				cbCtx.sendPluginResult(pluginResult);
			} catch (JSONException e) {
				String errMsg = e.getMessage();
				LOG.e(LOG_TAG, errMsg, e);
				PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errMsg);
				pluginResult.setKeepCallback(true);
				cbCtx.sendPluginResult(pluginResult);
			} finally {
				mLocationClient.stop();
			}
		}
	};

	/**
	 * 插件主入口
	 */
	@Override
	public boolean execute(String action, final JSONArray args, CallbackContext callbackContext) throws JSONException {
		LOG.d(LOG_TAG, "BaiduPush#execute");

		boolean ret = false;

		if ("getCurrentPosition".equalsIgnoreCase(action)) {
			cbCtx = callbackContext;

			PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
			pluginResult.setKeepCallback(true);
			cbCtx.sendPluginResult(pluginResult);

			if (mLocationClient == null) {
				mLocationClient = new LocationClient(this.webView.getContext());
				mLocationClient.registerLocationListener(myListener);

				// 配置定位SDK参数
				initLocation();
			}

			mLocationClient.start();
			ret = true;
		}

		return ret;
	}

	/**
	 * 配置定位SDK参数
	 */
	private void initLocation() {
		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationMode.Hight_Accuracy);// 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
		option.setCoorType("bd09ll");// 可选，默认gcj02，设置返回的定位结果坐标系
		option.setScanSpan(3000);// 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		option.setIsNeedAddress(true);// 可选，设置是否需要地址信息，默认不需要
		option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
		//option.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
		option.setLocationNotify(false);// 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
		option.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
		option.setIsNeedLocationPoiList(true);// 可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
		option.SetIgnoreCacheException(true);// 可选，默认false，设置是否收集CRASH信息，默认收集
		// option.setEnableSimulateGps(false);// 可选，默认false，设置是否需要过滤gps仿真结果，默认需要
		option.setOpenGps(true);// 可选，默认false,设置是否使用gps
		mLocationClient.setLocOption(option);
	}
}
