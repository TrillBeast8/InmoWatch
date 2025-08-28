
package com.example.inmocontrol_v2.hid;
import android.app.*; import android.bluetooth.*; import android.content.*; import android.os.*; import android.util.Log;
public class HidService extends Service {
    public static final String TAG="InmoHID"; private BluetoothAdapter adapter; private BluetoothHidDevice hid; private BluetoothDevice lastHost;
    public class LocalBinder extends Binder{ public HidService getService(){ return HidService.this; } }
    private final IBinder binder=new LocalBinder();
    public interface ClientHook{ void onReady(HidService s); } private static ClientHook hook; public static void setClientListener(ClientHook h){ hook=h; }
    @Override public void onCreate(){ super.onCreate(); adapter=BluetoothAdapter.getDefaultAdapter(); startForeground(1, buildNotif("Idle")); adapter.getProfileProxy(this, profListener, BluetoothProfile.HID_DEVICE); if(hook!=null) hook.onReady(this); }
    @Override public IBinder onBind(Intent i){ return binder; } @Override public int onStartCommand(Intent i,int f,int id){ return START_STICKY; }
    private final BluetoothProfile.ServiceListener profListener=new BluetoothProfile.ServiceListener(){ @Override public void onServiceConnected(int p,BluetoothProfile proxy){ hid=(BluetoothHidDevice)proxy; registerApp(); } @Override public void onServiceDisconnected(int p){ hid=null; } };
    private final BluetoothHidDeviceAppSdpSettings sdp=new BluetoothHidDeviceAppSdpSettings("InmoControl HID","Wear HID controller","Inmo",BluetoothHidDevice.SUBCLASS1_COMBO,getReportMap());
    private final BluetoothHidDevice.Callback cb=new BluetoothHidDevice.Callback(){ @Override public void onAppStatusChanged(BluetoothDevice dev, boolean registered){ if(registered){ lastHost=dev; autoConnectIfNeeded(); } }
        @Override public void onConnectionStateChanged(BluetoothDevice d,int s){ if(s==BluetoothProfile.STATE_CONNECTED){ lastHost=d; saveLast(d); updateNotif("Connected to "+d.getName()); } else if(s==BluetoothProfile.STATE_DISCONNECTED){ updateNotif("Disconnected"); autoConnectIfNeeded(); } } };
    private void registerApp(){ if(hid==null)return; hid.registerApp(sdp,null,null,Runnable::run,cb); }
    private void autoConnectIfNeeded(){ BluetoothDevice d=loadLast(); if(d!=null && hid!=null){ try{ hid.connect(d);}catch(Throwable t){ Log.e(TAG,"autoConnect failed",t);} } }
    // Public API (tailored from WearMouse concepts, replicated directly)
    public void sendText(String t){ if(hid==null||lastHost==null)return; for(char c: t.toCharArray()) sendKey(asciiToKey(c)); releaseKeys(); }
    public void leftClick(){ sendMouse((byte)1,0,0,0); sendMouse((byte)0,0,0,0); }
    public void rightClick(){ sendMouse((byte)2,0,0,0); sendMouse((byte)0,0,0,0); }
    public void move(int dx,int dy){ sendMouse((byte)0, clamp(dx), clamp(dy), 0); }
    public void wheel(int w){ sendMouse((byte)0, 0, 0, clamp(w)); }
    public void dpad(String dir){ int[] k; switch(dir){ case "up":k=new int[]{0x52};break; case "down":k=new int[]{0x51};break; case "left":k=new int[]{0x50};break; case "right":k=new int[]{0x4F};break; case "upleft":k=new int[]{0x52,0x50};break; case "upright":k=new int[]{0x52,0x4F};break; case "downleft":k=new int[]{0x51,0x50};break; case "downright":k=new int[]{0x51,0x4F};break; default:k=new int[]{0x28}; } sendCombo(k); releaseKeys(); }
    public void consumer(String w){ int u=0; switch(w){ case "playpause":u=0xCD;break; case "vol+":u=0xE9;break; case "vol-":u=0xEA;break; case "forward":u=0xB5;break; case "rewind":u=0xB6;break; case "mute":u=0xE2;break; } sendConsumer(u); }
    private void sendMouse(byte buttons, int dx, int dy, int wheel){ if(hid==null||lastHost==null)return; byte[] r=new byte[]{ buttons, (byte)dx, (byte)dy, (byte)wheel }; hid.sendReport(lastHost, 2, r); }
    private void sendCombo(int[] k){ if(hid==null||lastHost==null)return; byte[] rep=new byte[]{0,0,0,0,0,0,0,0}; for(int i=0;i<k.length&&i<6;i++){ rep[2+i]=(byte)k[i]; } hid.sendReport(lastHost,1,rep); }
    private void sendKey(int k){ sendCombo(new int[]{k}); } private void releaseKeys(){ if(hid==null||lastHost==null)return; byte[] r=new byte[]{0,0,0,0,0,0,0,0}; hid.sendReport(lastHost,1,r); }
    private void sendConsumer(int u){ if(hid==null||lastHost==null||u==0)return; byte[] r=new byte[]{(byte)(u&0xFF),(byte)((u>>8)&0xFF)}; hid.sendReport(lastHost,3,r); hid.sendReport(lastHost,3,new byte[]{0,0}); }
    private static int clamp(int v){ return Math.max(-127, Math.min(127, v)); }
    private static int asciiToKey(char c){ if(c>='a'&&c<='z')return 0x04+(c-'a'); if(c>='A'&&c<='Z')return 0x04+(c-'A'); if(c>='1'&&c<='9')return 0x1E+(c-'1'); if(c=='0')return 0x27; if(c==' ')return 0x2C; if(c=='\n')return 0x28; if(c=='.')return 0x37; if(c==',')return 0x36; if(c=='-')return 0x2D; return 0x2C; }
    private void saveLast(BluetoothDevice d){ getSharedPreferences("inmo",MODE_PRIVATE).edit().putString("lastHost", d.getAddress()).apply(); }
    private BluetoothDevice loadLast(){ String addr=getSharedPreferences("inmo",MODE_PRIVATE).getString("lastHost",null); if(addr==null)return null; return adapter.getRemoteDevice(addr); }
    private Notification buildNotif(String t){ NotificationManager nm=getSystemService(NotificationManager.class); String ch="hid"; if(Build.VERSION.SDK_INT>=26){ nm.createNotificationChannel(new NotificationChannel(ch,"Inmo HID",NotificationManager.IMPORTANCE_MIN)); }
        Notification.Builder b=Build.VERSION.SDK_INT>=26? new Notification.Builder(this,ch): new Notification.Builder(this); return b.setContentTitle("InmoControl").setContentText(t).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).build(); }
    private void updateNotif(String t){ getSystemService(NotificationManager.class).notify(1, buildNotif(t)); }
    private static byte[] getReportMap(){ return new byte[]{
        // Keyboard (ID 1)\n        (byte)0x05,0x01,(byte)0x09,0x06,(byte)0xA1,0x01,(byte)0x85,0x01,(byte)0x05,0x07,(byte)0x19,0x00,(byte)0x29,(byte)0x73,(byte)0x15,0x00,(byte)0x25,0x01,\n        (byte)0x75,0x01,(byte)0x95,0x08,(byte)0x81,0x02,(byte)0x95,0x01,(byte)0x75,0x08,(byte)0x81,0x01,(byte)0x95,0x05,(byte)0x75,0x01,(byte)0x05,0x08,(byte)0x19,0x01,(byte)0x29,0x05,(byte)0x91,0x02,\n        (byte)0x95,0x01,(byte)0x75,0x03,(byte)0x91,0x01,(byte)0x95,0x06,(byte)0x75,0x08,(byte)0x15,0x00,(byte)0x25,(byte)0x73,(byte)0x05,0x07,(byte)0x19,0x00,(byte)0x29,(byte)0x73,(byte)0x81,0x00,(byte)0xC0,\n        // Mouse (ID 2) with X,Y,Wheel\n        (byte)0x05,0x01,(byte)0x09,0x02,(byte)0xA1,0x01,(byte)0x85,0x02,(byte)0x09,0x01,(byte)0xA1,0x00,\n        (byte)0x05,0x09,(byte)0x19,0x01,(byte)0x29,0x03,(byte)0x15,0x00,(byte)0x25,0x01,(byte)0x95,0x03,(byte)0x75,0x01,(byte)0x81,0x02,\n        (byte)0x95,0x01,(byte)0x75,0x05,(byte)0x81,0x01,\n        (byte)0x05,0x01,(byte)0x09,0x30,(byte)0x09,0x31,(byte)0x09,0x38,\n        (byte)0x15,(byte)0x81,(byte)0x25,0x7F,(byte)0x75,0x08,(byte)0x95,0x03,(byte)0x81,0x06,\n        (byte)0xC0,(byte)0xC0,\n        // Consumer (ID 3)\n        (byte)0x05,0x0C,(byte)0x09,0x01,(byte)0xA1,0x01,(byte)0x85,0x03,(byte)0x15,0x00,(byte)0x26,(byte)0xFF,0x03,(byte)0x19,0x00,(byte)0x2A,(byte)0xFF,0x03,(byte)0x75,0x10,(byte)0x95,0x01,(byte)0x81,0x00,(byte)0xC0\n    }; }\n}
