package net.northprint.osmtest

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class MainActivity : AppCompatActivity() {

    private lateinit var mMapcontroller: IMapController

    //位置情報系
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null
    private var mLocationRequest: LocationRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext, PreferenceManager.getDefaultSharedPreferences(
                applicationContext
            )
        )

        // layout resourse
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // map
        mMapcontroller = mapView.controller
        mMapcontroller.setZoom(15.0)
        // 初期値は旭川
        val geoPoint = GeoPoint(43.7600, 142.3260)
        mMapcontroller.setCenter(geoPoint)

        // 位置情報利用
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 右下のボタン
        location_button.setOnClickListener {
            //現在地の表示
            mMapcontroller.animateTo(GeoPoint(mLastLocation!!.latitude, mLastLocation!!.longitude), 15.0, 1, 1.0f, true)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            //LocationRequest
            mLocationRequest = LocationRequest()
            mFusedLocationClient?.requestLocationUpdates(mLocationRequest, object : LocationCallback() {
                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    Log.d(TAG, "Location Available")
                }

                override fun onLocationResult(result: LocationResult) {
                    for (location: Location in result.locations) {
                        mLastLocation = location
                    }
                }
            }, null)
        }
    }

    // オプションメニューの作成
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // オプションメニューを選んだとき
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        mMapcontroller.animateTo(GeoPoint(43.7600, 142.3260), 15.0, 1)

        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 位置情報の許可をリクエスト
    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")

            showSnackbar(R.string.permission_rationale)
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest()
        }
    }

    // 位置情報の許可をチェック
    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    // 許可のチェックをリクエスト
    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    // 許可をチェックした結果
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> // Permission granted.
                    getLastLocation()
                else -> // Permission denied.

                    // Notify the user via a SnackBar that they have rejected a core permission for the
                    // app, which makes the Activity useless. In a real app, core permissions would
                    // typically be best requested during a welcome-screen flow.

                    // Additionally, it is important to remember that a permission might have been
                    // rejected without asking the user for permission (device policy or "Never ask
                    // again" prompts). Therefore, a user interface affordance is typically implemented
                    // when permissions are denied. Otherwise, your app could appear unresponsive to
                    // touches or interactions which have required permissions.

                    showSnackbar(R.string.permission_denied_explanation)
            }
        }
    }

    // 最新の位置情報の保存
    @SuppressLint("MissingPermission", "SetTextI18n")
    fun getLastLocation() {
        mFusedLocationClient!!.lastLocation
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    mLastLocation = task.result
                } else {
                    Log.w(TAG, "getLastLocation:exception", task.exception)
                }
            }
    }

    // スナックバー表示用
    private fun showSnackbar(mainTextStringId: Int) {
        Toast.makeText(this@MainActivity, getString(mainTextStringId), Toast.LENGTH_LONG).show()
    }

    // 定数
    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        private const val TAG = "MainActivity"

    }
}
