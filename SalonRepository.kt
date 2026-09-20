
package com.salonbeauty.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

data class Sale(val id: Long, val client: String, val service: String, val amount: Double, val currency: String)
data class Debt(val client: String, val amount: Double, val currency: String)
data class Employee(val id: Long = 0, val name: String, val salary: Double, val commission: Double)
data class Product(val id: Long = 0, val name: String, val quantity: Int, val minQuantity: Int)
data class ServiceItem(val id: Long=0, val name:String, val price:Double, val currency:String, val duration:Int)
data class ClientItem(val id:Long=0, val name:String, val phone:String, val note:String)
data class AppointmentItem(val id:Long=0, val client:String, val service:String, val date:String, val time:String, val status:String)
data class ExpenseItem(val id:Long=0, val title:String, val amount:Double, val currency:String, val date:String)

class SalonDb(context: Context) : SQLiteOpenHelper(context, "salon.db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,salary REAL NOT NULL,commission REAL NOT NULL)")
        db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,quantity INTEGER NOT NULL,minQuantity INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE debts(id INTEGER PRIMARY KEY AUTOINCREMENT,client TEXT NOT NULL,amount REAL NOT NULL,currency TEXT NOT NULL)")
        db.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY AUTOINCREMENT,client TEXT NOT NULL,service TEXT NOT NULL,amount REAL NOT NULL,currency TEXT NOT NULL,discount REAL NOT NULL DEFAULT 0,paid REAL NOT NULL DEFAULT 0,payment_method TEXT NOT NULL DEFAULT 'نقدي',employee TEXT NOT NULL DEFAULT '',cost REAL NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE services(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,price REAL NOT NULL,currency TEXT NOT NULL,duration INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT NOT NULL,note TEXT NOT NULL)")
        db.execSQL("CREATE TABLE appointments(id INTEGER PRIMARY KEY AUTOINCREMENT,client TEXT NOT NULL,service TEXT NOT NULL,date TEXT NOT NULL,time TEXT NOT NULL,status TEXT NOT NULL)")
        db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,amount REAL NOT NULL,currency TEXT NOT NULL,date TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if(oldVersion < 3){
            listOf(
                "ALTER TABLE sales ADD COLUMN discount REAL NOT NULL DEFAULT 0",
                "ALTER TABLE sales ADD COLUMN paid REAL NOT NULL DEFAULT 0",
                "ALTER TABLE sales ADD COLUMN payment_method TEXT NOT NULL DEFAULT 'نقدي'",
                "ALTER TABLE sales ADD COLUMN employee TEXT NOT NULL DEFAULT ''",
                "ALTER TABLE sales ADD COLUMN cost REAL NOT NULL DEFAULT 0"
            ).forEach { runCatching { db.execSQL(it) } }
        }
        if(oldVersion < 2){
            db.execSQL("CREATE TABLE IF NOT EXISTS services(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,price REAL NOT NULL,currency TEXT NOT NULL,duration INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS clients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT NOT NULL,note TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS appointments(id INTEGER PRIMARY KEY AUTOINCREMENT,client TEXT NOT NULL,service TEXT NOT NULL,date TEXT NOT NULL,time TEXT NOT NULL,status TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,amount REAL NOT NULL,currency TEXT NOT NULL,date TEXT NOT NULL)")
        }
    }
}

class SalonRepository(context: Context) {
    private val db = SalonDb(context.applicationContext).writableDatabase

    fun employees(): MutableList<Employee> = mutableListOf<Employee>().also { out ->
        db.rawQuery("SELECT id,name,salary,commission FROM employees ORDER BY id DESC", null).use { c ->
            while(c.moveToNext()) out.add(Employee(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3)))
        }
    }
    fun addEmployee(e: Employee) {
        db.insert("employees", null, ContentValues().apply { put("name",e.name); put("salary",e.salary); put("commission",e.commission) })
    }
    fun products(): MutableList<Product> = mutableListOf<Product>().also { out ->
        db.rawQuery("SELECT id,name,quantity,minQuantity FROM products ORDER BY id DESC", null).use { c ->
            while(c.moveToNext()) out.add(Product(c.getLong(0),c.getString(1),c.getInt(2),c.getInt(3)))
        }
    }
    fun addProduct(p: Product) {
        db.insert("products", null, ContentValues().apply { put("name",p.name); put("quantity",p.quantity); put("minQuantity",p.minQuantity) })
    }
    fun debts(): MutableList<Debt> = mutableListOf<Debt>().also { out ->
        db.rawQuery("SELECT client,amount,currency FROM debts ORDER BY id DESC", null).use { c ->
            while(c.moveToNext()) out.add(Debt(c.getString(0),c.getDouble(1),c.getString(2)))
        }
    }
    fun addDebt(d: Debt) {
        db.insert("debts", null, ContentValues().apply { put("client",d.client); put("amount",d.amount); put("currency",d.currency) })
    }
    fun addSale(s: Sale) {
        db.insert("sales", null, ContentValues().apply { put("client",s.client); put("service",s.service); put("amount",s.amount); put("currency",s.currency) })
    }
    fun sales(): MutableList<Sale> = mutableListOf<Sale>().also { out ->
        db.rawQuery("SELECT id,client,service,amount,currency FROM sales ORDER BY id DESC", null).use { c ->
            while(c.moveToNext()) out.add(Sale(c.getLong(0),c.getString(1),c.getString(2),c.getDouble(3),c.getString(4),c.getDouble(5),c.getDouble(6),c.getString(7),c.getString(8),c.getDouble(9)))
        }
    }



    fun services(): MutableList<ServiceItem> = mutableListOf<ServiceItem>().also { out ->
        db.rawQuery("SELECT id,name,price,currency,duration FROM services ORDER BY id DESC",null).use { c ->
            while(c.moveToNext()) out.add(ServiceItem(c.getLong(0),c.getString(1),c.getDouble(2),c.getString(3),c.getInt(4)))
        }
    }
    fun addService(x:ServiceItem){db.insert("services",null,ContentValues().apply{put("name",x.name);put("price",x.price);put("currency",x.currency);put("duration",x.duration)})}
    fun deleteService(id:Long){db.delete("services","id=?",arrayOf(id.toString()))}

    fun clients(): MutableList<ClientItem> = mutableListOf<ClientItem>().also { out ->
        db.rawQuery("SELECT id,name,phone,note FROM clients ORDER BY id DESC",null).use { c ->
            while(c.moveToNext()) out.add(ClientItem(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)))
        }
    }
    fun addClient(x:ClientItem){db.insert("clients",null,ContentValues().apply{put("name",x.name);put("phone",x.phone);put("note",x.note)})}
    fun deleteClient(id:Long){db.delete("clients","id=?",arrayOf(id.toString()))}

    fun appointments(): MutableList<AppointmentItem> = mutableListOf<AppointmentItem>().also { out ->
        db.rawQuery("SELECT id,client,service,date,time,status FROM appointments ORDER BY date,time",null).use { c ->
            while(c.moveToNext()) out.add(AppointmentItem(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5)))
        }
    }
    fun addAppointment(x:AppointmentItem){db.insert("appointments",null,ContentValues().apply{put("client",x.client);put("service",x.service);put("date",x.date);put("time",x.time);put("status",x.status)})}
    fun deleteAppointment(id:Long){db.delete("appointments","id=?",arrayOf(id.toString()))}

    fun expenses(): MutableList<ExpenseItem> = mutableListOf<ExpenseItem>().also { out ->
        db.rawQuery("SELECT id,title,amount,currency,date FROM expenses ORDER BY id DESC",null).use { c ->
            while(c.moveToNext()) out.add(ExpenseItem(c.getLong(0),c.getString(1),c.getDouble(2),c.getString(3),c.getString(4)))
        }}
    }

    fun addExpense(x:ExpenseItem){db.insert("expenses",null,ContentValues().apply{put("title",x.title);put("amount",x.amount);put("currency",x.currency);put("date",x.date)})}
    fun deleteExpense(id:Long){db.delete("expenses","id=?",arrayOf(id.toString()))}



    fun employeeCommission(employeeId:Long):Double {
        db.rawQuery("SELECT commission FROM employees WHERE id=?",arrayOf(employeeId.toString())).use{c->
            return if(c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }
    fun employeeByName(name:String):Employee? = employees().firstOrNull{it.name==name}

    fun updateService(x:ServiceItem){db.update("services",ContentValues().apply{put("name",x.name);put("price",x.price);put("currency",x.currency);put("duration",x.duration)},"id=?",arrayOf(x.id.toString()))}
    fun updateClient(x:ClientItem){db.update("clients",ContentValues().apply{put("name",x.name);put("phone",x.phone);put("note",x.note)},"id=?",arrayOf(x.id.toString()))}
    fun updateAppointment(x:AppointmentItem){db.update("appointments",ContentValues().apply{put("client",x.client);put("service",x.service);put("date",x.date);put("time",x.time);put("status",x.status)},"id=?",arrayOf(x.id.toString()))}
    fun updateExpense(x:ExpenseItem){db.update("expenses",ContentValues().apply{put("title",x.title);put("amount",x.amount);put("currency",x.currency);put("date",x.date)},"id=?",arrayOf(x.id.toString()))}
    fun deleteClient(id:Long){db.delete("clients","id=?",arrayOf(id.toString()))}
    fun deleteAppointment(id:Long){db.delete("appointments","id=?",arrayOf(id.toString()))}
    fun deleteExpense(id:Long){db.delete("expenses","id=?",arrayOf(id.toString()))}
    fun deleteSale(id:Long){db.delete("sales","id=?",arrayOf(id.toString()))}

    fun getSetting(key: String, defaultValue: String = ""): String {
        db.rawQuery("SELECT value FROM settings WHERE key=?", arrayOf(key)).use { c ->
            return if (c.moveToFirst()) c.getString(0) else defaultValue
        }
    }

    fun setSetting(key: String, value: String) {
        db.insertWithOnConflict(
            "settings", null,
            ContentValues().apply { put("key", key); put("value", value) },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun settingsMap(): Map<String,String> {
        val out = mutableMapOf<String,String>()
        db.rawQuery("SELECT key,value FROM settings", null).use { c ->
            while(c.moveToNext()) out[c.getString(0)] = c.getString(1)
        }
        return out
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("version", 5)
        root.put("settings", JSONObject().apply { settingsMap().forEach { (k,v) -> put(k,v) } })
        root.put("services", JSONArray().apply { services().forEach { put(JSONObject().apply { put("name",it.name);put("price",it.price);put("currency",it.currency);put("duration",it.duration) }) } })
        root.put("clients", JSONArray().apply { clients().forEach { put(JSONObject().apply { put("name",it.name);put("phone",it.phone);put("note",it.note) }) } })
        root.put("appointments", JSONArray().apply { appointments().forEach { put(JSONObject().apply { put("client",it.client);put("service",it.service);put("date",it.date);put("time",it.time);put("status",it.status) }) } })
        root.put("expenses", JSONArray().apply { expenses().forEach { put(JSONObject().apply { put("title",it.title);put("amount",it.amount);put("currency",it.currency);put("date",it.date) }) } })
        root.put("employees", JSONArray().apply { employees().forEach { put(JSONObject().apply {
            put("name",it.name); put("salary",it.salary); put("commission",it.commission)
        }) }})
        root.put("products", JSONArray().apply { products().forEach { put(JSONObject().apply {
            put("name",it.name); put("quantity",it.quantity); put("minQuantity",it.minQuantity)
        }) }})
        root.put("debts", JSONArray().apply { debts().forEach { put(JSONObject().apply {
            put("client",it.client); put("amount",it.amount); put("currency",it.currency)
        }) }})
        root.put("sales", JSONArray().apply { sales().forEach { put(JSONObject().apply {
            put("client",it.client); put("service",it.service); put("amount",it.amount); put("currency",it.currency)
        }) }})
        return root.toString(2)
    }

    fun importJson(json: String) {
        val root = JSONObject(json)
        db.beginTransaction()
        try {
            db.delete("employees",null,null); db.delete("products",null,null); db.delete("debts",null,null); db.delete("sales",null,null); db.delete("settings",null,null); db.delete("services",null,null); db.delete("clients",null,null); db.delete("appointments",null,null); db.delete("expenses",null,null)
            root.optJSONArray("employees")?.let { a -> for(i in 0 until a.length()) { val o=a.getJSONObject(i); addEmployee(Employee(name=o.getString("name"),salary=o.getDouble("salary"),commission=o.getDouble("commission"))) } }
            root.optJSONArray("products")?.let { a -> for(i in 0 until a.length()) { val o=a.getJSONObject(i); addProduct(Product(name=o.getString("name"),quantity=o.getInt("quantity"),minQuantity=o.getInt("minQuantity"))) } }
            root.optJSONArray("debts")?.let { a -> for(i in 0 until a.length()) { val o=a.getJSONObject(i); addDebt(Debt(o.getString("client"),o.getDouble("amount"),o.getString("currency"))) } }
            root.optJSONArray("sales")?.let { a -> for(i in 0 until a.length()) { val o=a.getJSONObject(i); addSale(Sale(client=o.getString("client"),service=o.getString("service"),amount=o.getDouble("amount"),currency=o.getString("currency"),discount=o.optDouble("discount",0.0),paid=o.optDouble("paid",0.0),paymentMethod=o.optString("payment_method","نقدي"),employee=o.optString("employee",""),cost=o.optDouble("cost",0.0))) } }
            root.optJSONObject("settings")?.let { o -> o.keys().forEach { k -> setSetting(k,o.getString(k)) } }
            root.optJSONArray("services")?.let { a -> for(i in 0 until a.length()){val o=a.getJSONObject(i);addService(ServiceItem(name=o.getString("name"),price=o.getDouble("price"),currency=o.getString("currency"),duration=o.getInt("duration"))) } }
            root.optJSONArray("clients")?.let { a -> for(i in 0 until a.length()){val o=a.getJSONObject(i);addClient(ClientItem(name=o.getString("name"),phone=o.getString("phone"),note=o.getString("note"))) } }
            root.optJSONArray("appointments")?.let { a -> for(i in 0 until a.length()){val o=a.getJSONObject(i);addAppointment(AppointmentItem(client=o.getString("client"),service=o.getString("service"),date=o.getString("date"),time=o.getString("time"),status=o.getString("status"))) } }
            root.optJSONArray("expenses")?.let { a -> for(i in 0 until a.length()){val o=a.getJSONObject(i);addExpense(ExpenseItem(title=o.getString("title"),amount=o.getDouble("amount"),currency=o.getString("currency"),date=o.getString("date"))) } }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
