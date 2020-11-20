package com.example.aclascheckscaledemo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;




import aclasdriver.AclasScale;

import aclasdriver.AclasScale.AclasScaleListener;
import aclasdriver.AclasScale.St_Data;
import aclasdriver.AclasScale.St_Plu;


import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity {
	 private static final String tag = "AclasCheckOutScale";
	    private AclasScale scale = null;
//	    private static boolean runflag;
	    private Button bTare, bZero,bTareRead;
	    private Button btnConfirm = null;
	    private Button btnClose = null;
	    private Button btnPreTare	= null;
	    private TextView tWei;
	    
	    private EditText etAddr;
		final private String strVer 	= "V2.217";//"V1.002";//
	    private boolean bFlagInit = false;
	    AclasScaleListener listener = null;
	  
		private St_Data m_Data = null;
		private String  m_strKeyPre = "--";
		private int		m_iKeyPre 	= -1;
		private int		m_iKeyDisplayCnt = 10;
		
		private EditText m_etRdTareVal 	= null;
		private EditText m_etPTVal = null;
		private String	m_strDeviceId		= "";
		private Button btnPluRead = null;
		private Button btnPluWrite = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        etAddr = (EditText)findViewById(R.id.etscaleAddr);

        String addrString  = readComAddr();
        
		bTare = (Button) findViewById(R.id.button_tare);
        bTare.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	if(scale!=null){
                    scale.SetTare();
//                    scale.setPreTare(20);
            	}else{
            		Log.d(tag, "scaler null");
            	}
            }
        });
        
        bTareRead = (Button) findViewById(R.id.button_tareread);
        bTareRead.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	if(scale!=null){
            		m_etRdTareVal.setText(" ");
                    scale.getTareValue();
            	}else{
            		Log.d(tag, "scaler null");
            	}
            }
        });
        
        btnConfirm = (Button)findViewById(R.id.button_confirm);
        btnConfirm.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				btnConfirm.setEnabled(false);
				btnClose.setEnabled(false);
				String addr = etAddr.getText().toString();
				saveComAddr(addr);
				openScale();
				Message msgMessage = myhandle.obtainMessage(0, MESSAGE_OPEN, 0);
				myhandle.sendMessage(msgMessage);
			}
		});
        
        btnClose = (Button)findViewById(R.id.button_close);
        btnClose.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				btnConfirm.setEnabled(false);
				btnClose.setEnabled(false);
            	CloseDevice();
				Message msgMessage = myhandle.obtainMessage(0, MESSAGE_CLOSE, 0);
				myhandle.sendMessage(msgMessage);
				
			}
		});
        
        bZero = (Button) findViewById(R.id.button_zero);
        bZero.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	if(scale!=null){
                    scale.SetZero();
            	}
            }
        });
        
        tWei = (TextView) findViewById(R.id.textView_wei);
        tWei.setText("------");
        
        //etAddr = (EditText)findViewById(R.id.etscaleAddr);
        Toast.makeText(this, "Version:"+strVer, Toast.LENGTH_SHORT).show();
        
        if(addrString!=null){
        	etAddr.setText(addrString);
        }
        
        listener = new AclasScale.AclasScaleListener(){
        	public void OnError(int code){
        		Log.e(tag, "OnError!!!!"+code);
        		Message msg = myhandle.obtainMessage();
                msg.arg1 = 0;
        		if(code==AclasScale.CODENEEDUPDATE){

	                msg.arg1 = 1;
    		        Log.d(tag, "AclasScale$AclasScaleListener onError CODENEEDUPDATE---------");
//    		        myhandle.sendEmptyMessage(0);
    		        myhandle.sendMessage(msg);
        		}else if(code==AclasScale.NODATARECEIVE){

	                msg.arg1 = 2;
    		        Log.d(tag, "AclasScale$AclasScaleListener onError NODATARECEIVE---------");
    		        myhandle.sendMessage(msg);
    		        
        		}else if(code==AclasScale.STREAMERROR){

	                msg.arg1 = 4;
    		        Log.d(tag, "AclasScale$AclasScaleListener onError STREAMERROR---------");
    		        myhandle.sendMessage(msg);
        		}else if(code==AclasScale.DISCONNECT){

	                msg.arg1 = 5;
    		        Log.d(tag, "AclasScale$AclasScaleListener onError DISCONNECT---------");
    		        myhandle.sendMessage(msg);
        		}
        	}
        	public void OnDataReceive(St_Data data){
        		if(data.m_iStatus==-1){
        			Log.d(tag, "data error");
        			m_Data	= null;
        		}else{
//        			Log.d(tag, "data --------------------------");
        			m_Data = data;
        			Message msg_read = myhandle.obtainMessage();
                    msg_read.arg1 = 6; 
                    int iType = ((Spinner)findViewById(R.id.spType)).getSelectedItemPosition();
                    if(iType==0){
                        msg_read.obj	= (data.m_iStatus==0?"U":"S")+" "+data.m_fWeight+data.m_strUnit+" id:"+m_strDeviceId;
                    }else{
                    	if(!data.m_stKey.m_strKey.isEmpty()||m_iKeyDisplayCnt--==0){
                    		m_strKeyPre		= data.m_stKey.m_strKey;
                    		m_iKeyPre		= data.m_stKey.m_iValue;
                    		m_iKeyDisplayCnt	= 10;
                    	}
                        msg_read.obj	= (data.m_iStatus==0?"U":"S")+" "+String.format("%.3f",data.m_fWeight)+data.m_strUnit
                        		+" "+String.format("%.3f",data.m_fPrice)+" "+String.format("%.3f",data.m_fTotal)+" id:"+m_strDeviceId
            					+" key:"+m_iKeyPre+" str:"+m_strKeyPre;
                    }
                    myhandle.sendMessage(msg_read);
        			//tWei.setText((data.m_iStatus==0?"U":"S")+" "+data.m_fWeight+data.m_strUnit+" "+data.m_fPrice+" "+data.m_fTotal);
	        		Log.d(tag, "data:"+(data.m_iStatus==0?"Unstable":"Stable")+" weight:"+String.format("%.3f",data.m_fWeight)
	        				+" price:"+String.format("%.3f",data.m_fPrice)+" total:"
        			+data.m_fTotal+" key:"+data.m_stKey.m_iValue+" str:"+data.m_stKey.m_strKey);
        		}
        	}
			
			public void OnReadTare(float fVal,boolean bFlag){
				String string = bFlag?String.valueOf(fVal):"Error";
				Message msg = Message.obtain(myhandle, 0, 7, 0,string );
				myhandle.sendMessageAtFrontOfQueue(msg);
				Log.d(tag, "data len OnReadTare:"+bFlag+" "+fVal);
			}
        };
        
        btnPreTare = (Button)findViewById(R.id.button_pretare);

        btnPreTare.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	if(scale!=null){
            		String strVal = m_etPTVal.getText().toString();
                    scale.setPreTare(Integer.valueOf(strVal));
            	}
            }
        });

		m_etPTVal = (EditText)findViewById(R.id.etscalePreTareVal);
        m_etRdTareVal	= (EditText)findViewById(R.id.etscaleReadTareVal);
        

        Button btnReadStart = (Button)findViewById(R.id.button_readstart);

        btnReadStart.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	if(scale!=null){
            		scale.StartRead();
            	}
            }
        });
        Button btnReadStop = (Button)findViewById(R.id.button_readstop);

        btnReadStop.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	if(scale!=null){
            		scale.StopRead();
//            		scale.AclasChangeProtocol();
            	}
            }
        });
        //-------------------------------------
        btnPluRead	= (Button)findViewById(R.id.button_pluread);
        btnPluRead.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	if(scale!=null){
            		readPlu();
            	}
            }
        });

        btnPluWrite	= (Button)findViewById(R.id.button_pluwrite);
        btnPluWrite.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
            	if(scale!=null){
            		writePlu();
            	}
            }
        });
        

        initComSpin(addrString);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	  @Override
	    protected void onResume() {
	        // TODO Auto-generated method stub
	        super.onResume();
	        Log.d(tag, "scale onresume");
//	            openScale();
	    }
	  
	  private void CloseDevice(){

	        if (scale != null){
	        	scale.StopRead();
	        	scale.close();
	        	scale = null;
	        }
	  }
	    
	    @Override
	    protected void onPause() {
	        // TODO Auto-generated method stub
	        super.onPause();
//	        MainActivity.runflag = false;
	        CloseDevice();
	        //scale.close();
	        Log.d("scale", "scale--->onPause");
	    }
	    
	    @Override
	    protected void onDestroy(){
	        super.onDestroy();
//	        MainActivity.runflag = false;
	        CloseDevice();
	        Log.d("scale", "scale--->onDestroy");

	    }
	    
	    private void openScale() {
	    	try {
//	    		MainActivity.runflag	= false;
	    		
	    		CloseDevice();
	    		
	        	String strAdd = etAddr.getText().toString();
	            Log.d(tag, "scale new aclas scale:"+strAdd);
	            int iType = ((Spinner)findViewById(R.id.spType)).getSelectedItemPosition();
	            scale = new AclasScale(new File(strAdd),iType,listener);
	            scale.bLogFlag	= true;
	            scale.open();
	            m_strDeviceId	= scale.GetId();
//	            btnReadAd.setVisibility(iType==0?View.INVISIBLE:View.VISIBLE);
	            
	        }
	        catch (SecurityException e) {
	            // TODO Auto-generated catch block
	        	scale	= null;
	        	Toast.makeText(this,"OpenScale exception", Toast.LENGTH_SHORT).show();
	            e.printStackTrace();
	        }
	    	catch (Exception e) {
				// TODO: handle exception
	    		scale	= null;
	        	Toast.makeText(this,"OpenScale exception", Toast.LENGTH_SHORT).show();
	            e.printStackTrace();
			}
	        if(scale!=null){
		        Log.d(tag, "scale start run thread");
		        //scale.setAclasScaleListener(arg0)
		       // scale.setAclasScaleListener(listener);
		        	scale.StartRead();
	        }else{
		        Log.e(tag, "scale null!!!!!!!!!!!!!!!!!!!");
	        }
		}
	    

	    private static final int BASE				= 8;
		private static final int MESSAGE_OPEN 		= 1+BASE;
		private static final int MESSAGE_CLOSE 	    = 2+BASE;
	    public Handler myhandle = new Handler(){
	    	public void handleMessage(Message msg) {
	    		switch (msg.arg1) {
				case 1:
		    		Toast.makeText(MainActivity.this,"The Firmware need update", Toast.LENGTH_LONG).show();
		    		  scale.StopRead();
					break;
				case 2:
		    		Toast.makeText(MainActivity.this,"There is no data!", Toast.LENGTH_LONG).show();
		    		  scale.StopRead();
					break;
                case 4:
		    		  Toast.makeText(MainActivity.this,"Stream channel error!", Toast.LENGTH_LONG).show();
		    		  CloseDevice();
              	  break;
                case 5:
		    		  Toast.makeText(MainActivity.this,"Scaler disconnect!", Toast.LENGTH_LONG).show();
		    		  CloseDevice();
		    		  
		    		  break;
                case 6:
                	
                	 tWei.setText((String)msg.obj);
                	 if(m_Data!=null){
//                    	 CheckStableWeight();
                	 }
                	break;	
                case 7:
                	m_etRdTareVal.setText((String)msg.obj);
               	break;	
                case MESSAGE_OPEN:
                	btnClose.setEnabled(true);
                	break;
                case MESSAGE_CLOSE:
                	btnConfirm.setEnabled(true);
                	tWei.setText("");
                	break;
    			
				default:
		    		Toast.makeText(MainActivity.this,"Unknown error", Toast.LENGTH_LONG).show();
					break;
				}
	    	}
	    };
	    
	    /*
	     * save com address in txt file;
	     */
	    private void saveComAddr(String name) {
	    	if(name.length()>0){
	    		File fDown 			= Environment.getExternalStorageDirectory();
		    	String strDown		= fDown.getAbsolutePath();
		    	Log.d(tag, "Download path:"+strDown);
		    	final String LOG_FILE = strDown+"/Download/Aclas/settings.txt";
				try {
					
					Log.d(tag, "write file path:"+LOG_FILE);
					File file = new File(LOG_FILE);
					if(file.exists()){
						file.delete();
					}
					file.createNewFile();
					FileWriter writer = new FileWriter(file, true);
					writer.write(name, 0,name.length());
					writer.close();
				} catch (Exception e) {
					
					// TODO: handle exception
					Log.e(tag, "saveComAddr error:"+e.toString());
				}
	    	}
		}
	   
	    private String readComAddr() {

	    	File fDown 			= Environment.getExternalStorageDirectory();
	    	String strDown		= fDown.getAbsolutePath();
	    	Log.d(tag, "Download path:"+strDown);
	    	final String LOG_FILE = strDown+"/Download/Aclas/settings.txt";
	        //String LOG_FILE = "/storage/sdcard0/Download/settings.txt";//"/storage/emulate.0/Download/setting.txt"
			Log.d(tag, "read file path:"+LOG_FILE);
	    	String addrName = null;
			File file = new File(LOG_FILE);
			if(file.exists()){

				try {
					char[] buffer = new char[1024];
					
					FileReader	reader = new FileReader(file);
					int iCnt = reader.read(buffer);
					if( -1!=iCnt ){
						addrName = String.valueOf(buffer, 0, iCnt);
					}

					reader.close();
				} catch (Exception e) {
					// TODO: handle exception
					Log.e(tag, "readComAddr error:"+e.toString());
				}
				
			}
			return addrName;
		}
	     private void initComSpin(String str) {

				final List<String> list 	= AclasScale.getAvailableUartList();
				Spinner spCom 		= (Spinner)findViewById(R.id.spCom);
				if(list.size()>0){

		            ArrayAdapter<String> adapter_Com = new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line);
		            adapter_Com.addAll(list);
		            adapter_Com.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		            spCom.setAdapter(adapter_Com);
				}
				if(str!=null){
					for(int i=0;i<list.size();i++)
						if(str.compareTo(list.get(i))==0){
							spCom.setSelection(i);
							break;
						}
				}
				spCom.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
		            public void onItemSelected(AdapterView<?> parent, View view,
		                    int pos, long id) {

						if(bFlagInit){

							String strCom 	= list.get(pos);
							etAddr.setText(strCom);

				        	Log.d(tag, "scale msg spCom:"+strCom);
						}else{
							bFlagInit = true;
						}
					}

		            @Override
		            public void onNothingSelected(AdapterView<?> parent) {
		                
		            }
				});
				
				Spinner spType		= (Spinner)findViewById(R.id.spType);

				spType.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
		            public void onItemSelected(AdapterView<?> parent, View view,
		                    int pos, long id) {
						if(pos==0){
							btnPluRead.setVisibility(View.GONE);
							btnPluWrite.setVisibility(View.GONE);
							bTareRead.setVisibility(View.VISIBLE);
							btnPreTare.setVisibility(View.VISIBLE);
							m_etPTVal.setVisibility(View.VISIBLE);
							m_etRdTareVal.setVisibility(View.VISIBLE);
						}else{
							btnPluRead.setVisibility(View.VISIBLE);
							btnPluWrite.setVisibility(View.VISIBLE);
							bTareRead.setVisibility(View.GONE);
							btnPreTare.setVisibility(View.GONE);
							m_etPTVal.setVisibility(View.GONE);
							m_etRdTareVal.setVisibility(View.GONE);
						}

					}

		            @Override
		            public void onNothingSelected(AdapterView<?> parent) {
		                
		            }
				});
		}
	     
	     private void writePlu(){
	    	 ArrayList<St_Plu>  plus = new ArrayList<AclasScale.St_Plu>();
	    	 for(int i=0;i<70;i++){
	    		 St_Plu	plu = scale.new St_Plu();
	    		 plu.m_iIndex	= i+1;
	    		 plu.m_iPrice	= (i+1)*111;
	    		 plus.add(plu);
	    	 }
	    	 scale.sendPluData(plus);
	    	 Toast.makeText(this, "writePlu!!!!!", Toast.LENGTH_SHORT).show();
	     }
	     
	     private void readPlu(){
	    	 ArrayList<St_Plu>  plus = new ArrayList<AclasScale.St_Plu>();
	    	 for(int i=0;i<70;i++){
	    		 St_Plu	plu = scale.new St_Plu();
	    		 plu.m_iIndex	= i+1;
	    		 plus.add(plu);
	    	 }
	    	 scale.readPluData(plus);

	    	 Toast.makeText(this, "readPlu!!!!!", Toast.LENGTH_SHORT).show();
	    	 ArrayList<String>  list 	= new ArrayList<String>();
	    	 for(int i=0;i<plus.size();i++){
	    		 St_Plu	plu = plus.get(i);
	    		 String info = "PLU"+plu.m_iIndex+":"+plu.m_iPrice+"\r"+"\n";
	    		 list.add(info);
	    		 Log.d(tag, "readPlu"+plu.m_iIndex+":"+plu.m_iPrice);
	    	 }
	    	 String strFileName	= "PLU"+getDateString()+".txt";
	    	 saveDataToTxt(strFileName,list);
	     }
	     

	 	private void saveDataToTxt(String strFileName,ArrayList<String> list){
	 		if(strFileName!=null&&!strFileName.isEmpty()&&!list.isEmpty()){
	 			try {

//	 				String strData = new String(strDataIn.getBytes(),"UTF-16");
//	 				String strData = stringToUniconde(strDataIn);
	 	    		File fDown 			= Environment.getExternalStorageDirectory();
	 		    	String strDown		= fDown.getAbsolutePath();
//	 		    	Log.d(tag, "Download path:"+strDown);
	 		    	String aclaString	= strDown+"/Download/Aclas/";
	 		    	final String LOG_FILE = strDown+"/Download/Aclas/"+strFileName;
	 		    	
	 		    	try {
	 					File  fold = new File(aclaString);
	 					if(!fold.exists()){
	 						fold.mkdir();
	 					}
	 				} catch (Exception e) {
	 					// TODO: handle exception
	 					e.printStackTrace();
	 				}
	 				try {
	 					
//	 					Log.d(tag, "write file path:"+LOG_FILE);
	 					File file = new File(LOG_FILE);
	 					if(!file.exists()){
	 						file.createNewFile();
	 						String cmd = "chmod 777 "+file.getAbsolutePath();
	 						Runtime runtime = Runtime.getRuntime();
	 						java.lang.Process proc = runtime.exec(cmd);
	 					}
	 					BufferedWriter w  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,true),"UTF-16"));
	 					for (int i = 0; i < list.size(); i++) {
	 						String strData	= list.get(i);
	 						w.write(strData);
	 					}
	 					w.flush();
	 					w.close();
	 				} catch (Exception e) {
	 					
	 					// TODO: handle exception
	 					Log.e(tag, "saveDataToTxt error:"+e.toString());
	 				}
	 			} catch (Exception e) {
	 				// TODO: handle exception
	 			}
	 		}
	 	}
	 	

	    
	    private String getDateString(){

			String FORM_STRING = "yyyyMMddHHmmss";
			SimpleDateFormat date = new SimpleDateFormat(FORM_STRING,Locale.getDefault());
			return date.format(new java.util.Date());
	    }
	 	
}
