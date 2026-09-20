
package com.salonbeauty.app

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Pink=Color(0xFFB62D61); private val SoftPink=Color(0xFFFFEEF4); private val Green=Color(0xFF16835A); private val Orange=Color(0xFFE58B2A)

data class Client(val name:String,val phone:String,val due:Double)
data class Service(val name:String,val price:Double,val currency:String)
data class Expense(val title:String,val amount:Double,val currency:String)

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{SalonApp()}}
}

@Composable
fun SalonApp(){
    val context=LocalContext.current
    val auth=remember{AuthManager(context)}
    var logged by remember{mutableStateOf(auth.isLoggedIn())}
    MaterialTheme(colorScheme=lightColorScheme(primary=Pink,secondary=Color(0xFF8E3D63),background=Color(0xFFFFFAFC))){
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl){
            if(!logged) LoginScreen{user,pass-> if(auth.login(user,pass)) logged=true else Toast.makeText(context,"بيانات الدخول غير صحيحة",Toast.LENGTH_SHORT).show()}
            else SalonMain(auth){logged=false}
        }
    }
}

@Composable
fun LoginScreen(onLogin:(String,String)->Unit){
    var user by remember{mutableStateOf("")}; var pass by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().background(SoftPink).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        Icon(Icons.Default.ContentCut,null,tint=Pink,modifier=Modifier.size(70.dp))
        Text("صالون الجمال",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Pink)
        Text("إدارة حسابات صالون التجميل",color=Color.Gray)
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(user,{user=it},modifier=Modifier.fillMaxWidth(),label={Text("اسم المستخدم")},singleLine=true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(pass,{pass=it},modifier=Modifier.fillMaxWidth(),label={Text("كلمة المرور")},singleLine=true)
        Spacer(Modifier.height(18.dp))
        Button({onLogin(user,pass)},modifier=Modifier.fillMaxWidth().height(52.dp)){Text("تسجيل الدخول")}
        Spacer(Modifier.height(10.dp))
        Text("الدخول الأول: admin / 1234",fontSize=12.sp,color=Color.Gray)
    }
}

@Composable
fun SalonMain(auth:AuthManager,onLogout:()->Unit){
    val context=LocalContext.current
    val repo=remember{SalonRepository(context)}
    var screen by remember{mutableStateOf("home")}; var invoice by remember{mutableStateOf(false)}
    val clients=remember{mutableStateListOf(Client("سارة أحمد","0933 123 456",0.0),Client("لينا محمد","0944 567 890",25.0),Client("ديمة خالد","0987 654 321",0.0),Client("نور علي","0955 432 198",15.0))}
    val services=remember{mutableStateListOf(Service("قص شعر",25.0,"USD"),Service("صبغة شعر",60.0,"USD"),Service("استشوار",20.0,"USD"),Service("مانيكير",15.0,"USD"),Service("تصفيف وتسريحة",40.0,"USD"))}
    val expenses=remember{mutableStateListOf(Expense("إيجار المحل",200.0,"USD"),Expense("رواتب الموظفات",600.0,"USD"),Expense("مواد تجميل",150.0,"USD"))}
    var employees by remember{mutableStateOf(repo.employees())}; var products by remember{mutableStateOf(repo.products())}; var debts by remember{mutableStateOf(repo.debts())}
    var dbServices by remember{mutableStateOf(repo.services())}; var dbClients by remember{mutableStateOf(repo.clients())}; var dbAppointments by remember{mutableStateOf(repo.appointments())}; var dbExpenses by remember{mutableStateOf(repo.expenses())}

    val createBackup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri:Uri?-> uri?.let{context.contentResolver.openOutputStream(it)?.use{o->o.write(repo.exportJson().toByteArray())};Toast.makeText(context,"تم حفظ النسخة الاحتياطية",Toast.LENGTH_SHORT).show()}}
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?-> uri?.let{try{val json=context.contentResolver.openInputStream(it)!!.bufferedReader().readText();repo.importJson(json);employees=repo.employees();products=repo.products();debts=repo.debts();Toast.makeText(context,"تمت الاستعادة",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(context,"ملف النسخة الاحتياطية غير صالح",Toast.LENGTH_SHORT).show()}}}

    Scaffold(bottomBar={NavigationBar(containerColor=Color.White){NavItem("الرئيسية",Icons.Default.Home,screen=="home"){screen="home"};NavItem("المواعيد",Icons.Default.Event,screen=="appointments"){screen="appointments"};NavItem("العميلات",Icons.Default.Person,screen=="clients"){screen="clients"};NavItem("التقارير",Icons.Default.Assessment,screen=="reports"){screen="reports"}}},floatingActionButton={if(screen=="home")FloatingActionButton({invoice=true},containerColor=Pink,contentColor=Color.White){Icon(Icons.Default.Add,"فاتورة")}}){padding->
        Box(Modifier.padding(padding).fillMaxSize()){
            when(screen){
                "home"->HomeScreen({screen=it},{invoice=true})
                "appointments"->AppointmentsScreen(dbAppointments,services){ client,service,date,time -> repo.addAppointment(AppointmentItem(client=client,service=service,date=date,time=time,status="مؤكد")); dbAppointments=repo.appointments() }
                "clients"->ClientsScreen(dbClients){name,phone,note->repo.addClient(ClientItem(name=name,phone=phone,note=note));dbClients=repo.clients()}
                "reports"->ReportsScreen(repo)
            "new_sale"->NewSaleScreen(repo,repo.services(),repo.employees()){ }
            "alerts"->AlertsScreen(repo)
                "services"->ServicesScreen(dbServices){name,price,currency,duration->repo.addService(ServiceItem(name=name,price=price,currency=currency,duration=duration));dbServices=repo.services()}
                "expenses"->ExpensesScreen(dbExpenses){title,amount,currency,date->repo.addExpense(ExpenseItem(title=title,amount=amount,currency=currency,date=date));dbExpenses=repo.expenses()}
                "employees"->EmployeesScreen(employees){repo.addEmployee(Employee(name="موظفة جديدة",salary=300.0,commission=10.0));employees=repo.employees()}
                "inventory"->InventoryScreen(products){repo.addProduct(Product(name="منتج جديد",quantity=10,minQuantity=2));products=repo.products()}
                "debts"->DebtsScreen(debts)
                "settings"->SettingsScreen({createBackup.launch("salon_backup.json")},{restore.launch(arrayOf("application/json"))},{auth.logout();onLogout()})
            }
        }
    }
    if(invoice)InvoiceDialog(services,repo){invoice=false}
}

@Composable fun NavItem(l:String,i:androidx.compose.ui.graphics.vector.ImageVector,s:Boolean,c:()->Unit)=NavigationBarItem(s,c,icon={Icon(i,l)},label={Text(l,fontSize=11.sp)})
@Composable fun HomeScreen(nav:(String)->Unit,newInvoice:()->Unit){
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFFFFFAFC)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("صالون الجمال",fontSize=28.sp,fontWeight=FontWeight.Bold,color=Pink);Text("إدارة حسابات صالون التجميل",color=Color.Gray)}
        item{Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=SoftPink)){Column(Modifier.padding(18.dp).fillMaxWidth()){Text("ملخص اليوم",fontWeight=FontWeight.Bold,fontSize=18.sp);Spacer(Modifier.height(12.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Stat("المبيعات","$ 1,250",Pink);Stat("المصروفات","$ 320",Orange);Stat("الصافي","$ 930",Green)};Spacer(Modifier.height(8.dp));Text("2,500,000 ل.س  •  $350  •  €120",fontSize=12.sp)}}}
        item{Card(shape=RoundedCornerShape(18.dp)){Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("جاهزية اليوم",fontWeight=FontWeight.Bold);Text("إدارة المواعيد والفواتير والمصروفات من مكان واحد",fontSize=12.sp,color=Color.Gray)};Icon(Icons.Default.CheckCircle,null,tint=Green,modifier=Modifier.size(34.dp))}}}
        item{Text("الإدارة",fontWeight=FontWeight.Bold,fontSize=18.sp)}
        item{GridRow(listOf("المواعيد" to Icons.Default.Event,"الفواتير" to Icons.Default.ReceiptLong),listOf("appointments","invoice"),nav,newInvoice)}
        item{GridRow(listOf("العميلات" to Icons.Default.People,"الخدمات" to Icons.Default.ContentCut),listOf("clients","services"),nav,newInvoice)}
        item{GridRow(listOf("الموظفات" to Icons.Default.Badge,"المخزون" to Icons.Default.Inventory2),listOf("employees","inventory"),nav,newInvoice)}
        item{GridRow(listOf("المصروفات" to Icons.Default.Payments,"الديون" to Icons.Default.AccountBalanceWallet),listOf("expenses","debts"),nav,newInvoice)}
        item{GridRow(listOf("التقارير" to Icons.Default.Assessment,"الإعدادات" to Icons.Default.Settings),listOf("reports","settings"),nav,newInvoice)}
    }
}
@Composable fun GridRow(a:List<Pair<String,androidx.compose.ui.graphics.vector.ImageVector>>,targets:List<String>,nav:(String)->Unit,newInvoice:()->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){a.forEachIndexed{i,(t,ic)->Quick(t,ic){if(targets[i]=="invoice")newInvoice()else nav(targets[i])}}}}
@Composable fun Stat(t:String,v:String,c:Color)=Column(horizontalAlignment=Alignment.CenterHorizontally){Text(t,fontSize=12.sp,color=Color.Gray);Text(v,fontWeight=FontWeight.Bold,color=c,fontSize=16.sp)}
@Composable fun Quick(t:String,i:androidx.compose.ui.graphics.vector.ImageVector,c:()->Unit)=Card(Modifier.weight(1f).height(82.dp).clickable{c()},shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Icon(i,t,tint=Pink);Text(t,fontSize=13.sp)}}
@Composable fun TopBar(t:String)=Row(Modifier.fillMaxWidth().padding(16.dp)){Text(t,fontSize=25.sp,fontWeight=FontWeight.Bold,color=Pink)}


@Composable fun AppointmentsScreen(l:List<AppointmentItem>, services:List<Service>, add:(String,String,String,String)->Unit){
    var show by remember{mutableStateOf(false)}; var q by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize()){TopBar("المواعيد"); OutlinedTextField(q,{q=it},Modifier.fillMaxWidth().padding(horizontal=16.dp),label={Text("بحث عن عميلة أو خدمة")});Button({show=true},Modifier.padding(horizontal=16.dp).fillMaxWidth()){Text("إضافة موعد")}
        LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(l.filter{it.client.contains(q,true) || it.service.contains(q,true)}){a->Card{Column(Modifier.padding(14.dp)){Text("${a.date}  ${a.time}",fontWeight=FontWeight.Bold,color=Pink);Text("${a.client} • ${a.service}");Text(a.status,color=Green,fontSize=12.sp)}}}}}
    if(show) AddAppointmentDialog(services){c,s,d,t->add(c,s,d,t);show=false}
}
@Composable fun AddAppointmentDialog(services:List<Service>,onSave:(String,String,String,String)->Unit){
    var client by remember{mutableStateOf("")};var date by remember{mutableStateOf("")};var time by remember{mutableStateOf("")};var service by remember{mutableStateOf(services.firstOrNull()?.name?:"")}
    AlertDialog(onDismissRequest={},title={Text("موعد جديد")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(client,{client=it},label={Text("اسم العميلة")});OutlinedTextField(date,{date=it},label={Text("التاريخ مثال 2026-09-20")});OutlinedTextField(time,{time=it},label={Text("الوقت")});Text("الخدمة");services.take(5).forEach{s->Row(Modifier.clickable{service=s.name},verticalAlignment=Alignment.CenterVertically){RadioButton(service==s.name,{service=s.name});Text(s.name)}}}},confirmButton={Button({if(client.isNotBlank())onSave(client,service,date,time)}){Text("حفظ")}},dismissButton={TextButton({}){Text("إلغاء")}})
}
@Composable fun ClientsScreen(l:List<ClientItem>,add:(String,String,String)->Unit){
    var show by remember{mutableStateOf(false)}; var q by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize()){TopBar("العميلات"); OutlinedTextField(q,{q=it},Modifier.fillMaxWidth().padding(horizontal=16.dp),label={Text("بحث")});Button({show=true},Modifier.padding(horizontal=16.dp).fillMaxWidth()){Text("إضافة عميلة")}
        LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(l.filter{it.name.contains(q,true) || it.phone.contains(q,true)}){c->Card{Column(Modifier.padding(14.dp)){Text(c.name,fontWeight=FontWeight.Bold);Text(c.phone,color=Color.Gray);if(c.note.isNotBlank())Text(c.note,fontSize=12.sp)}}}}}
    if(show)AddClientDialog{n,p,note->add(n,p,note);show=false}
}
@Composable fun AddClientDialog(onSave:(String,String,String)->Unit){
    var n by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest={},title={Text("عميلة جديدة")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(n,{n=it},label={Text("الاسم")});OutlinedTextField(p,{p=it},label={Text("الهاتف")});OutlinedTextField(note,{note=it},label={Text("ملاحظات")})}},confirmButton={Button({if(n.isNotBlank())onSave(n,p,note)}){Text("حفظ")}},dismissButton={TextButton({}){Text("إلغاء")}})
}
@Composable fun ServicesScreen(l:List<ServiceItem>,add:(String,Double,String,Int)->Unit){
    var show by remember{mutableStateOf(false)}; var q by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize()){TopBar("الخدمات"); OutlinedTextField(q,{q=it},Modifier.fillMaxWidth().padding(horizontal=16.dp),label={Text("بحث عن خدمة")});Button({show=true},Modifier.padding(horizontal=16.dp).fillMaxWidth()){Text("إضافة خدمة")}
        LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(l.filter{it.name.contains(q,true)}){s->Card{Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(s.name,fontWeight=FontWeight.Bold);Text("${s.duration} دقيقة",fontSize=12.sp,color=Color.Gray)};Text("${s.price} ${s.currency}",color=Pink,fontWeight=FontWeight.Bold)}}}}}}
    if(show)AddServiceDialog{n,p,c,d->add(n,p,c,d);show=false}
}
@Composable fun AddServiceDialog(onSave:(String,Double,String,Int)->Unit){
    var n by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};var c by remember{mutableStateOf("USD")};var d by remember{mutableStateOf("60")}
    AlertDialog(onDismissRequest={},title={Text("خدمة جديدة")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(n,{n=it},label={Text("اسم الخدمة")});OutlinedTextField(p,{p=it},label={Text("السعر")});OutlinedTextField(d,{d=it},label={Text("المدة بالدقائق")});Row{listOf("SYP","USD","EUR").forEach{x->FilterChip(c==x,{c=x},{Text(x)})}}}},confirmButton={Button({val price=p.toDoubleOrNull()?:0.0;val dur=d.toIntOrNull()?:60;if(n.isNotBlank())onSave(n,price,c,dur)}){Text("حفظ")}},dismissButton={TextButton({}){Text("إلغاء")}})
}
@Composable fun ExpensesScreen(l:List<ExpenseItem>,add:(String,Double,String,String)->Unit){
    var show by remember{mutableStateOf(false)}; var q by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize()){TopBar("المصروفات"); OutlinedTextField(q,{q=it},Modifier.fillMaxWidth().padding(horizontal=16.dp),label={Text("بحث")});Button({show=true},Modifier.padding(horizontal=16.dp).fillMaxWidth()){Text("إضافة مصروف")}
        LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(l.filter{it.title.contains(q,true)}){e->Card{Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(e.title,fontWeight=FontWeight.Bold);Text(e.date,fontSize=12.sp,color=Color.Gray)};Text("${e.amount} ${e.currency}",color=Pink,fontWeight=FontWeight.Bold)}}}}}}
    if(show)AddExpenseDialog{t,a,c,d->add(t,a,c,d);show=false}
}
@Composable fun AddExpenseDialog(onSave:(String,Double,String,String)->Unit){
    var t by remember{mutableStateOf("")};var a by remember{mutableStateOf("")};var c by remember{mutableStateOf("USD")};var d by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest={},title={Text("مصروف جديد")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(t,{t=it},label={Text("البند")});OutlinedTextField(a,{a=it},label={Text("المبلغ")});OutlinedTextField(d,{d=it},label={Text("التاريخ")});Row{listOf("SYP","USD","EUR").forEach{x->FilterChip(c==x,{c=x},{Text(x)})}}}},confirmButton={Button({val amount=a.toDoubleOrNull()?:0.0;if(t.isNotBlank())onSave(t,amount,c,d)}){Text("حفظ")}},dismissButton={TextButton({}){Text("إلغاء")}})
}



@Composable fun AlertsScreen(repo:SalonRepository){
    val appointments=repo.appointments()
    val products=repo.products()
    val debts=repo.debts()
    val low=products.filter{it.quantity<=it.minQuantity}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        TopBar("التنبيهات")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            if(low.isEmpty() && debts.isEmpty()) item{Card{Text("لا توجد تنبيهات مهمة حاليًا.",Modifier.padding(16.dp),color=Green)}}
            if(low.isNotEmpty()) item{AlertCard("مخزون منخفض","${low.size} منتج يحتاج إلى إعادة الطلب")}
            if(debts.isNotEmpty()) item{AlertCard("ديون العملاء","يوجد ${debts.size} سجل دين يحتاج إلى متابعة")}
            if(appointments.isNotEmpty()) item{AlertCard("المواعيد","لديك ${appointments.size} موعد مسجل")}
        }
    }
}
@Composable fun AlertCard(title:String,text:String){
    Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.Bold,color=Pink);Text(text,color=Color.Gray)}}
}



@Composable fun NewSaleScreen(repo:SalonRepository, services:List<ServiceItem>, employees:List<Employee>, onSaved:()->Unit){
    var client by remember{mutableStateOf("")}; var service by remember{mutableStateOf(services.firstOrNull()?.name?:"")}
    var employee by remember{mutableStateOf(employees.firstOrNull()?.name?:"")}; var amount by remember{mutableStateOf("")}
    var discount by remember{mutableStateOf("0")}; var paid by remember{mutableStateOf("")}
    var cost by remember{mutableStateOf("0")}; var currency by remember{mutableStateOf(repo.getSetting("default_currency","USD"))}
    var payment by remember{mutableStateOf("نقدي")}; var done by remember{mutableStateOf(false)}
    val svc=services.firstOrNull{it.name==service}; val gross=amount.toDoubleOrNull()?:svc?.price?:0.0
    val disc=discount.toDoubleOrNull()?:0.0; val net=(gross-disc).coerceAtLeast(0.0)
    val paidValue=paid.toDoubleOrNull()?:0.0; val due=(net-paidValue).coerceAtLeast(0.0)
    val costValue=cost.toDoubleOrNull()?:0.0
    val emp=employees.firstOrNull{it.name==employee}; val commission=net*((emp?.commission?:0.0)/100.0)
    val profit=net-costValue-commission
    Column(Modifier.fillMaxSize().padding(16.dp)){TopBar("فاتورة جديدة")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){
            item{OutlinedTextField(client,{client=it},Modifier.fillMaxWidth(),label={Text("اسم العميلة")})}
            item{Text("الخدمة",fontWeight=FontWeight.Bold)}
            item{services.forEach{s->Row(Modifier.clickable{service=s.name}.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){RadioButton(service==s.name,{service=s.name});Text("${s.name} — ${s.price} ${s.currency}")}}}
            item{OutlinedTextField(amount,{amount=it},Modifier.fillMaxWidth(),label={Text("السعر")},singleLine=true)}
            item{OutlinedTextField(discount,{discount=it},Modifier.fillMaxWidth(),label={Text("الخصم")},singleLine=true)}
            item{OutlinedTextField(paid,{paid=it},Modifier.fillMaxWidth(),label={Text("المبلغ المدفوع")},singleLine=true)}
            item{OutlinedTextField(cost,{cost=it},Modifier.fillMaxWidth(),label={Text("تكلفة الخدمة")},singleLine=true)}
            item{Text("العملة")}; item{Row{listOf("SYP","USD","EUR").forEach{c->FilterChip(currency==c,{currency=c},{Text(c)})}}}
            item{Text("طريقة الدفع",fontWeight=FontWeight.Bold)}; item{Row{listOf("نقدي","بطاقة","تحويل").forEach{x->FilterChip(payment==x,{payment=x},{Text(x)})}}}
            item{Text("الموظفة",fontWeight=FontWeight.Bold)}
            item{employees.forEach{e->Row(Modifier.clickable{employee=e.name}.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){RadioButton(employee==e.name,{employee=e.name});Text("${e.name} — ${e.commission}%")}}}
            item{Card{Column(Modifier.padding(14.dp)){Text("ملخص الفاتورة",fontWeight=FontWeight.Bold,color=Pink);Text("الإجمالي: $gross $currency");Text("الخصم: $disc $currency");Text("الصافي: $net $currency");Text("المدفوع: $paidValue $currency");Text("المتبقي: $due $currency",fontWeight=FontWeight.Bold);Text("العمولة: ${"%.2f".format(commission)} $currency");Text("صافي الربح التقريبي: ${"%.2f".format(profit)} $currency",color=Green)}}}
            item{Button({repo.addSale(Sale(client=client,service=service,amount=net,currency=currency,discount=disc,paid=paidValue,paymentMethod=payment,employee=employee,cost=costValue));done=true;onSaved()},Modifier.fillMaxWidth()){Text("حفظ الفاتورة")}}
            if(done)item{Text("تم حفظ الفاتورة.",color=Green)}
        }
    }
}


@Composable fun ReportsScreen(repo:SalonRepository){
    val sales=repo.sales(); val totals=currencyTotal(sales)
    val expenses=repo.expenses(); val sypE=expenses.filter{it.currency=="SYP"}.sumOf{it.amount}; val usdE=expenses.filter{it.currency=="USD"}.sumOf{it.amount}; val eurE=expenses.filter{it.currency=="EUR"}.sumOf{it.amount}
    val due=mapOf("SYP" to sales.filter{it.currency=="SYP"}.sumOf{(it.amount-it.paid).coerceAtLeast(0.0)},"USD" to sales.filter{it.currency=="USD"}.sumOf{(it.amount-it.paid).coerceAtLeast(0.0)},"EUR" to sales.filter{it.currency=="EUR"}.sumOf{(it.amount-it.paid).coerceAtLeast(0.0)})
    val profit=mapOf("SYP" to sales.filter{it.currency=="SYP"}.sumOf{it.amount-it.cost},"USD" to sales.filter{it.currency=="USD"}.sumOf{it.amount-it.cost},"EUR" to sales.filter{it.currency=="EUR"}.sumOf{it.amount-it.cost})
    Column(Modifier.fillMaxSize().padding(16.dp)){TopBar("التقارير والأرباح");LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{ReportCard("المبيعات",listOf("ل.س" to totals.syp,"USD" to totals.usd,"EUR" to totals.eur),Pink)}
        item{ReportCard("المصروفات",listOf("ل.س" to sypE,"USD" to usdE,"EUR" to eurE),Orange)}
        item{ReportCard("المبالغ المتبقية",listOf("ل.س" to due["SYP"]!!,"USD" to due["USD"]!!,"EUR" to due["EUR"]!!),Pink)}
        item{ReportCard("ربح الخدمات قبل المصروفات العامة",listOf("ل.س" to profit["SYP"]!!,"USD" to profit["USD"]!!,"EUR" to profit["EUR"]!!),Green)}
        item{ReportCard("عدد الفواتير","${sales.size}",Green)}
    }}
}
@Composable fun ReportCard(title:String, values:List<Pair<String,Any>>, color:Color){
    Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=SoftPink)){
        Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.Bold,fontSize=18.sp,color=color);values.forEach{(k,v)->Text("$k: $v",fontSize=17.sp)}}
    }
}
@Composable fun ReportCard(title:String, value:String, color:Color){
    Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=SoftPink)){
        Column(Modifier.padding(16.dp)){Text(title,fontWeight=FontWeight.Bold,fontSize=18.sp,color=color);Text(value,fontSize=24.sp,fontWeight=FontWeight.Bold)}
    }
}

@Composable fun EmployeesScreen(l:List<Employee>,add:()->Unit){Column(Modifier.fillMaxSize()){TopBar("الموظفات");Button(add,Modifier.padding(horizontal=16.dp).fillMaxWidth()){Text("إضافة موظفة")};LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(l){e->Card{Column(Modifier.padding(16.dp)){Text(e.name,fontWeight=FontWeight.Bold);Text("الراتب: $${e.salary} • العمولة: ${e.commission}%")}}}}}}}
@Composable fun InventoryScreen(l:List<Product>,add:()->Unit){Column(Modifier.fillMaxSize()){TopBar("المخزون");Button(add,Modifier.padding(horizontal=16.dp).fillMaxWidth()){Text("إضافة منتج")};LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(l){p->Card{Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(p.name);Text("الكمية: ${p.quantity}",color=if(p.quantity<=p.minQuantity)Pink else Green)}}}}}}}
@Composable fun DebtsScreen(l:List<Debt>){Column(Modifier.fillMaxSize()){TopBar("الديون والمدفوعات");if(l.isEmpty())Text("لا توجد ديون مسجلة",Modifier.padding(20.dp),color=Color.Gray);LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(l){d->Card{Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(d.client);Text("${d.amount} ${d.currency}",color=Pink,fontWeight=FontWeight.Bold)}}}}}}}

@Composable
fun SettingsScreen(repo: SalonRepository, backup:()->Unit, restore:()->Unit, logout:()->Unit) {
    var salonName by remember { mutableStateOf(repo.getSetting("salon_name","صالون الجمال")) }
    var phone by remember { mutableStateOf(repo.getSetting("phone","")) }
    var address by remember { mutableStateOf(repo.getSetting("address","")) }
    var currency by remember { mutableStateOf(repo.getSetting("default_currency","USD")) }
    var usdToSyp by remember { mutableStateOf(repo.getSetting("usd_to_syp","")) }
    var eurToSyp by remember { mutableStateOf(repo.getSetting("eur_to_syp","")) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopBar("الإعدادات")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            item {
                Card(shape=RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        Text("بيانات الصالون", fontWeight=FontWeight.Bold, fontSize=18.sp, color=Pink)
                        OutlinedTextField(salonName,{salonName=it},Modifier.fillMaxWidth(),label={Text("اسم الصالون")},singleLine=true)
                        OutlinedTextField(phone,{phone=it},Modifier.fillMaxWidth(),label={Text("رقم الهاتف")},singleLine=true)
                        OutlinedTextField(address,{address=it},Modifier.fillMaxWidth(),label={Text("العنوان")},singleLine=true)
                    }
                }
            }
            item {
                Card(shape=RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(9.dp)) {
                        Text("العملات وسعر الصرف", fontWeight=FontWeight.Bold, fontSize=18.sp, color=Pink)
                        Text("العملات المتاحة: SYP / USD / EUR", color=Color.Gray)
                        Text("العملة الافتراضية")
                        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                            listOf("SYP","USD","EUR").forEach { c ->
                                FilterChip(currency==c,{currency=c},{Text(c)})
                            }
                        }
                        OutlinedTextField(usdToSyp,{usdToSyp=it},Modifier.fillMaxWidth(),label={Text("سعر 1 دولار بالليرة السورية")},singleLine=true)
                        OutlinedTextField(eurToSyp,{eurToSyp=it},Modifier.fillMaxWidth(),label={Text("سعر 1 يورو بالليرة السورية")},singleLine=true)
                    }
                }
            }
            item {
                Button(onClick={
                    repo.setSetting("salon_name",salonName)
                    repo.setSetting("phone",phone)
                    repo.setSetting("address",address)
                    repo.setSetting("default_currency",currency)
                    repo.setSetting("usd_to_syp",usdToSyp)
                    repo.setSetting("eur_to_syp",eurToSyp)
                    saved=true
                }, modifier=Modifier.fillMaxWidth()) { Text("حفظ الإعدادات") }
            }
            item { if(saved) Text("تم حفظ التغييرات بنجاح.", color=Green) }
            item {
                Card(shape=RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("النسخ الاحتياطي",fontWeight=FontWeight.Bold,fontSize=18.sp,color=Pink)
                        Button(backup,Modifier.fillMaxWidth()){Text("تصدير نسخة احتياطية")}
                        OutlinedButton(restore,Modifier.fillMaxWidth()){Text("استعادة نسخة احتياطية")}
                    }
                }
            }
            item {
                OutlinedButton(logout,Modifier.fillMaxWidth()){Text("تسجيل الخروج")}
            }
        }
    }
}

@Composable fun InvoiceDialog(services:List<Service>,repo:SalonRepository,onDismiss:()->Unit){
    val context=LocalContext.current;var selected by remember{mutableStateOf(services.first())};var currency by remember{mutableStateOf("USD")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("فاتورة جديدة",fontWeight=FontWeight.Bold)},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Text("اختيار الخدمة");services.take(4).forEach{s->Row(Modifier.fillMaxWidth().clickable{selected=s}.padding(3.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected==s,{selected=s});Text("${s.name} — ${s.price} ${s.currency}")}};Text("العملة");Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("SYP","USD","EUR").forEach{c->FilterChip(currency==c,{currency=c},{Text(c)})}}}},confirmButton={Button(onClick={repo.addSale(Sale(0,"عميلة نقدية",selected.name,selected.price,currency));createInvoicePdf(context,selected.name,selected.price,currency,repo.getSetting("salon_name","صالون الجمال"),repo.getSetting("phone",""),repo.getSetting("address",""));Toast.makeText(context,"تم حفظ الفاتورة وإنشاء PDF",Toast.LENGTH_SHORT).show();onDismiss()}){Text("حفظ ومشاركة PDF")}},dismissButton={TextButton(onClick=onDismiss){Text("إلغاء")}})
}

fun createInvoicePdf(context:android.content.Context,service:String,amount:Double,currency:String,salonName:String,phone:String,address:String){
    val doc=PdfDocument();val page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create());val p=Paint();p.textSize=22f;p.isFakeBoldText=true
    page.canvas.drawText(salonName,60f,70f,p);p.textSize=16f;p.isFakeBoldText=false
    page.canvas.drawText("Service: $service",60f,120f,p);page.canvas.drawText("Amount: $amount $currency",60f,155f,p);page.canvas.drawText("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}",60f,175f,p);page.canvas.drawText("Phone: $phone",60f,190f,p);page.canvas.drawText("Address: $address",60f,215f,p);page.canvas.drawText("Thank you",60f,250f,p)
    doc.finishPage(page);val file=File(context.cacheDir,"invoice_${System.currentTimeMillis()}.pdf");file.outputStream().use{doc.writeTo(it)};doc.close()
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file);context.startActivity(Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)})
}
