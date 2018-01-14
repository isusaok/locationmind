package com.example.frank.locationmind

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v4.app.ActivityCompat
import android.view.KeyEvent

import java.util.ArrayList

/**
 * Created by frank on 18/1/13.
 * 高德地图
 */

open class CheckPermissionsActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    /**
     * 需要进行检测的权限数组
     */
    protected var needPermissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)//Manifest.permission.WRITE_EXTERNAL_STORAGE,
    //Manifest.permission.READ_EXTERNAL_STORAGE,
    //Manifest.permission.READ_PHONE_STATE

    /**
     * 判断是否需要检测，防止不停的弹框
     */
    private var isNeedCheck = true

    override fun onResume() {
        super.onResume()
        if (isNeedCheck) {
            checkPermissions(*needPermissions)
        }
    }

    /*
     *
     * @param needRequestPermissonList
     * @since 2.5.0
     *
     */
    private fun checkPermissions(vararg permissions: String) {
        val needRequestPermissonList = findDeniedPermissions(permissions)
        if (null != needRequestPermissonList && needRequestPermissonList.size > 0) {
            ActivityCompat.requestPermissions(this,
                    needRequestPermissonList.toTypedArray(),
                    PERMISSON_REQUESTCODE)
        }
    }

    /**
     * 获取权限集中需要申请权限的列表
     *
     * @param permissions
     * @return
     * @since 2.5.0
     */
    private fun findDeniedPermissions(permissions: Array<out String>): List<String> {
        val needRequestPermissonList = ArrayList<String>()
        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(this,
                    perm) != PackageManager.PERMISSION_GRANTED || ActivityCompat.shouldShowRequestPermissionRationale(
                    this, perm)) {
                needRequestPermissonList.add(perm)
            }
        }
        return needRequestPermissonList
    }

    /**
     * 检测是否说有的权限都已经授权
     * @param grantResults
     * @return
     * @since 2.5.0
     */
    private fun verifyPermissions(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, paramArrayOfInt: IntArray) {
        if (requestCode == PERMISSON_REQUESTCODE) {
            if (!verifyPermissions(paramArrayOfInt)) {
                showMissingPermissionDialog()
                isNeedCheck = false
            }
        }
    }

    /**
     * 显示提示信息
     *
     * @since 2.5.0
     */
    private fun showMissingPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.notifyTitle)
        builder.setMessage(R.string.notifyMsg)

        // 拒绝, 退出应用
        builder.setNegativeButton(R.string.cancel
        ) { dialog, which -> finish() }

        builder.setPositiveButton(R.string.setting
        ) { dialog, which -> startAppSettings() }

        builder.setCancelable(false)

        builder.show()
    }

    /**
     * 启动应用的设置
     *
     * @since 2.5.0
     */
    private fun startAppSettings() {
        val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + packageName)
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {

        private val PERMISSON_REQUESTCODE = 0
    }


}