package virtual.camera.app.data

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.hack.opensdk.HackApi
import virtual.camera.app.R
import virtual.camera.app.app.App
import virtual.camera.app.app.AppManager
import virtual.camera.app.bean.AppInfo
import virtual.camera.app.bean.InstalledAppBean
import virtual.camera.app.util.AbiUtils
import virtual.camera.app.util.getString
import java.io.File


class AppsRepository {
    val TAG: String = "AppsRepository"
    private var mInstalledList = mutableListOf<AppInfo>()

    fun previewInstallList() {
        synchronized(mInstalledList) {
            val installedApplications: List<ApplicationInfo> = try {
                App.getContext().packageManager.getInstalledApplications(0)
            } catch (e: Throwable) {
                Log.e(TAG, "getInstalledApplications failed", e)
                emptyList()
            }

            val installedList = mutableListOf<AppInfo>()
            for (installedApplication in installedApplications) {
                try {
                    val file = File(installedApplication.sourceDir ?: continue)

                    if ((installedApplication.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

                    if (!AbiUtils.isSupport(file)) continue

                    val info = AppInfo(
                        installedApplication.loadLabel(App.getContext().packageManager).toString(),
                        installedApplication.loadIcon(App.getContext().packageManager),
                        installedApplication.packageName,
                        installedApplication.sourceDir,
                        false
                    )
                    installedList.add(info)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error loading app info for ${installedApplication.packageName}", e)
                }
            }
            this.mInstalledList.clear()
            this.mInstalledList.addAll(installedList)
        }
    }

    fun getInstalledAppList(
        userID: Int,
        loadingLiveData: MutableLiveData<Boolean>,
        appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        loadingLiveData.postValue(true)
        synchronized(mInstalledList) {
            Log.d(TAG, mInstalledList.joinToString(","))
            val newInstalledList = mInstalledList.map {
                val isInstalled = try {
                    HackApi.getPackageInfo(it.packageName, userID, 0) != null
                } catch (e: Throwable) {
                    false
                }
                InstalledAppBean(it.name, it.icon, it.packageName, it.sourceDir, isInstalled)
            }
            appsLiveData.postValue(newInstalledList)
            loadingLiveData.postValue(false)
        }
    }

    fun getInstalledModuleList(
        loadingLiveData: MutableLiveData<Boolean>,
        appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        loadingLiveData.postValue(true)
        synchronized(mInstalledList) {
            val moduleList = mInstalledList.filter { it.isXpModule }.map {
                InstalledAppBean(it.name, it.icon, it.packageName, it.sourceDir, false)
            }
            appsLiveData.postValue(moduleList)
            loadingLiveData.postValue(false)
        }
    }

    fun getVmInstallList(userId: Int, appsLiveData: MutableLiveData<List<AppInfo>>) {
        val sortListData = AppManager.mRemarkSharedPreferences.getString("AppList$userId", "")
        val sortList = sortListData?.split(",")

        val installedPkgs: MutableList<String> = try {
            HackApi.getInstalledPackages(0, userId) ?: mutableListOf()
        } catch (e: Throwable) {
            Log.e(TAG, "getInstalledPackages failed", e)
            mutableListOf()
        }

        installedPkgs.remove("com.waxmoon.ma.gp")

        val applicationList = mutableListOf<ApplicationInfo>()
        installedPkgs.forEach { pkg ->
            try {
                val packageInfo = HackApi.getPackageInfo(pkg, userId, 0)
                val appInfo = packageInfo?.applicationInfo
                if (appInfo != null) {
                    applicationList.add(appInfo)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "getPackageInfo failed for $pkg", e)
            }
        }

        val appInfoList = mutableListOf<AppInfo>()
        applicationList.also {
            if (!sortList.isNullOrEmpty()) {
                it.sortWith(AppsSortComparator(sortList))
            }
        }.forEach {
            try {
                val info = AppInfo(
                    it.loadLabel(App.getContext().packageManager).toString(),
                    it.loadIcon(App.getContext().packageManager),
                    it.packageName,
                    it.sourceDir,
                    isInstalledXpModule(it.packageName)
                )
                appInfoList.add(info)
            } catch (e: Throwable) {
                Log.e(TAG, "Error loading VM app info for ${it.packageName}", e)
            }
        }

        appsLiveData.postValue(appInfoList)
    }

    private fun isInstalledXpModule(packageName: String): Boolean = false

    fun installApk(source: String, userId: Int, resultLiveData: MutableLiveData<String>) {
        val installResult = try {
            HackApi.installPackageFromHost(source, userId, false)
        } catch (e: Throwable) {
            Log.e(TAG, "installPackageFromHost failed", e)
            -1
        }
        Log.e(TAG, "source:$source, installResult:$installResult")
        val INSTALL_SUCCEEDED = 1
        if (installResult == INSTALL_SUCCEEDED) {
            updateAppSortList(userId, source, true)
            resultLiveData.postValue(getString(R.string.install_success))
        } else {
            resultLiveData.postValue(getString(R.string.install_fail, "code:$installResult"))
        }
    }

    fun unInstall(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            HackApi.uninstallPackage(packageName, userID)
            updateAppSortList(userID, packageName, false)
            resultLiveData.postValue(getString(R.string.uninstall_success))
        } catch (e: Throwable) {
            Log.e(TAG, "unInstall failed for $packageName", e)
            resultLiveData.postValue(getString(R.string.uninstall_fail))
        }
    }

    fun launchApk(packageName: String, userId: Int, launchLiveData: MutableLiveData<Boolean>) {
        try {
            val intent: Intent? = HackApi.getLaunchIntentForPackage(packageName, userId)
            if (intent == null) {
                Log.e(TAG, "getLaunchIntentForPackage returned null for $packageName")
                launchLiveData.postValue(false)
                return
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            val result = HackApi.startActivity(intent, 0) == 0
            launchLiveData.postValue(result)
        } catch (e: Throwable) {
            Log.e(TAG, "launchApk failed for $packageName", e)
            launchLiveData.postValue(false)
        }
    }

    fun clearApkData(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            HackApi.deletePackageData(packageName, userID)
            resultLiveData.postValue(getString(R.string.clear_success))
        } catch (e: Throwable) {
            Log.e(TAG, "clearApkData failed for $packageName", e)
        }
    }

    private fun updateAppSortList(userID: Int, pkg: String, isAdd: Boolean) {
        val savedSortList = AppManager.mRemarkSharedPreferences.getString("AppList$userID", "")
        val sortList = linkedSetOf<String>()
        if (!savedSortList.isNullOrEmpty()) {
            sortList.addAll(savedSortList.split(",").filter { it.isNotEmpty() })
        }
        if (isAdd) sortList.add(pkg) else sortList.remove(pkg)
        AppManager.mRemarkSharedPreferences.edit {
            putString("AppList$userID", sortList.joinToString(","))
        }
    }

    fun updateApkOrder(userID: Int, dataList: List<AppInfo>) {
        AppManager.mRemarkSharedPreferences.edit {
            putString("AppList$userID", dataList.joinToString(",") { it.packageName })
        }
    }
}
