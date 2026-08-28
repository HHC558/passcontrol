package com.hhc558.passcontrol

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.hhc558.passcontrol.crypto.CryptoManager
import com.hhc558.passcontrol.data.AccountDao
import com.hhc558.passcontrol.data.AppDatabase
import com.hhc558.passcontrol.data.VaultRepository
import com.hhc558.passcontrol.xlsx.DiffEngine
import com.hhc558.passcontrol.xlsx.ImportDiff
import com.hhc558.passcontrol.xlsx.XlsxService
import kotlinx.coroutines.flow.MutableStateFlow

/** 手动依赖注入容器。 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val prefs = appContext.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
    val crypto = CryptoManager()
    private val database: AppDatabase = Room.databaseBuilder(appContext, AppDatabase::class.java, "passcontrol.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    val accountDao: AccountDao = database.accountDao()
    val vaultRepository = VaultRepository(prefs, crypto, accountDao)
    val xlsxService = XlsxService()
    val diffEngine = DiffEngine

    /** 跨页面共享的导入差异与结果消息（应用级单例状态）。 */
    val importFlow = MutableStateFlow<ImportDiff?>(null)
    val toastMessage = MutableStateFlow<String?>(null)
}

class PassControlApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}