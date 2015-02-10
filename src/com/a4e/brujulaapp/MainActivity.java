/**
 * Título: Android4Education Brujula App
 * Licencia Pública General de GNU (GPL) versión 3 
 * Código Fuente: https://github.com/pebosch/a4e-brujulaApp
 * Autor: Pedro Fernández Bosch
 * Fecha de la última modificación: 28/01/2015
 */

package com.a4e.brujulaapp;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import com.a4e.brujulaapp.R;

/**
 * Declaración de variables
 */
public class MainActivity extends Activity implements SensorEventListener {

	TextView txtAngle, txtFeedback;
	private ImageView imgCompass;
	
	private final String ruta_foto = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Brujula/"; // (Pictures/Brujula/) Ruta donde se almacenan las fotografías
	private File file = new File(ruta_foto);
	
	private RadioGroup rdGroup; // Botones de los puntos cardinales

	private float currentDegree = 0f; // Ángulo (grado) actual del compass

	private SensorManager mSensorManager; // Sensor manager del dispositivo
	
	// Los dos sensores que son necesarios (TYPE_ORINETATION está deprecated)
	Sensor accelerometer;
	Sensor magnetometer;
	
	float degree;
	float azimut;
	float rb_degree; //Radio Buttom Degree. Grados para hacer el calculo matematico (+0=Norte; +90=Este; -90=Oeste; -180=Sur)
	float[] mGravity;
	float[] mGeomagnetic;

	/**
	 * OnCreate Method Override 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Variables que almacenan los elementos del layout
		imgCompass = (ImageView) findViewById(R.id.imgViewCompass);		
		txtAngle = (TextView) findViewById(R.id.txtAngle);
		txtFeedback = (TextView) findViewById(R.id.txtFeedback);
		txtFeedback.setText("Encuentra el Norte magnético"); // Inicialización de txtFeedback

		// Inicialización de los sensores del dispositivo android para la brujula
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	    accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    
	    mGravity = null;
	    mGeomagnetic = null;
	    
	    // Declaración de los Radio Buttons
	    rdGroup = (RadioGroup) findViewById(R.id.rdGrp);
	    rdGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
	    	@Override
	        public void onCheckedChanged(RadioGroup group, int checkedId) {
	            // TODO Auto-generated method stub
	            if (checkedId == R.id.norte){
	            	rb_degree = 0;
	            	txtFeedback.setText("Encuentra el Norte magnético");
	            }else if (checkedId == R.id.sur){
	            	rb_degree = -180;
	            	txtFeedback.setText("Encuentra el Sur magnético");
	            }else if (checkedId == R.id.este){
	            	rb_degree = 90;
	            	txtFeedback.setText("Encuentra el Este magnético");
	            }else if (checkedId == R.id.oeste){
	            	rb_degree = -90;
	            	txtFeedback.setText("Encuentra el Oeste magnético");
	            }	            	
	        }
        });
	    
	 // Crea la carpeta donde se guardaran las fotografías en caso de que no exista
	 file.mkdirs();	
	}

	/**
	 * Registro de un listener para los sensores del accelerometer y el magnetometer
	 */	
	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
	    mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}

	/**
	 * Detención del listener para no malgastar la bateria
	 */	
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	/**
	 * Cálculo de posicionamiento del azimut
	 */	
	@Override
	public void onSensorChanged(SensorEvent event) {		
		// Se comprueba que tipo de sensor está activo en cada momento
		switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				mGravity = event.values.clone();
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				mGeomagnetic = event.values.clone();
				break;
		}
		
		// Direccionamiento del azimut
		if ((mGravity != null) && (mGeomagnetic != null)) {
		  	float RotationMatrix[] = new float[16];
		   	boolean success = SensorManager.getRotationMatrix(RotationMatrix, null, mGravity, mGeomagnetic);
		   	if (success) {
		   		float orientation[] = new float[3];
		   		SensorManager.getOrientation(RotationMatrix, orientation);
		   		azimut = orientation[0] * (180 / (float) Math.PI);		   				   		
		   	}
        }
		degree = azimut;
		if(degree > rb_degree && degree < rb_degree+1){
				// Abrir camara y guardar la foto
				String file = ruta_foto + getCode() + ".jpg";
				File mi_foto = new File( file );
				try {
					mi_foto.createNewFile();
				} catch (IOException ex) {              
					Log.e("ERROR ", "Error:" + ex);
				}       
				//
				Uri uri = Uri.fromFile( mi_foto );
				Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE); // Abre la camara para tomar la foto
				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri); // Guarda imagen
				startActivityForResult(cameraIntent, 0); // Retorna a la actividad
		}
		
		// Mostrar por pantalla el ángulo entre 0º y 360º
		double degree_aux = degree;
		if (degree_aux < 0)
			degree_aux = degree_aux * (-1);
		else if (degree_aux > 0)
			degree_aux = ((degree_aux - 180) * (-1)) + 180;
		
		txtAngle.setText("Ángulo: " + Double.toString(Math.round(degree_aux*100.0)/100.0) + "º");
		
		// Animación de la rottación (se revierte el giro en grados, negativo)
		RotateAnimation ra = new RotateAnimation(
				currentDegree+rb_degree, 
				degree+rb_degree,
				Animation.RELATIVE_TO_SELF, 0.5f, 
				Animation.RELATIVE_TO_SELF,
				0.5f);
		// Tiempo durante el cual la animación se llevará a cabo
		ra.setDuration(1000);
		
		// Establecer la animación después del final de la estado de reserva
		ra.setFillAfter(true);
		
		// Inicio de la animacion
		imgCompass.startAnimation(ra);
		currentDegree = -degree;
	}

	/**
	 * Calculo de cambios en la precision
	 */	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// No se usa
	}
	
	/**
	* Metodo privado que genera un codigo único para la fotografía segun la fecha y hora del sistema
	* @return photoCode 
	**/
	@SuppressLint("SimpleDateFormat")
	private String getCode(){
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
		String date = dateFormat.format(new Date() );
		String photoCode = "IMG_" + date;  
		return photoCode;
	}
	
	/**
	 * Definición del menu de opciones de la aplicación
	 */	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * Funcionalidad de los ítems del menu
	 */	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.optionLicencia:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.gnu.org/licenses/gpl.html")));
			break;
		case R.id.optionCodigo:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/pebosch/a4e-brujulaapp")));
			break;
		case R.id.optionAyuda:
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.a4e.brujulaapp")));
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}