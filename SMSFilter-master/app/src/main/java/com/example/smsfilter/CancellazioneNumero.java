package com.example.smsfilter;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CancellazioneNumero extends AppCompatActivity {

    private FirebaseFirestore cloud_db ;
    private Spinner spinner2 ;
    private String email ;
    private ArrayList<String> nomi ;
    private ArrayList<String> numeri ;
    private ArrayAdapter<String> dataAdapter ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancellazione_numero);

        Bundle bundle_email = getIntent().getExtras() ;
        if ( bundle_email != null )
            email = bundle_email.getString("user_email") ;

        cloud_db = FirebaseFirestore.getInstance() ;
        spinner2 = (Spinner) findViewById(R.id.spinner2) ;
        nomi = new ArrayList<String>() ;
        numeri = new ArrayList<String>() ;
        dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, nomi);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(dataAdapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 100);

        }

        else {

            loadContacts();
        }

        DBHelper db = new DBHelper(this ) ;

        EditText numero = (EditText) findViewById(R.id.editTextTextPersonName2) ;
        Button btn = (Button) findViewById(R.id.button2) ;

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String sel_name = (String) spinner2.getSelectedItem();
                int index = nomi.indexOf(sel_name) ;
                String ph_number = numeri.get(index) ;
                numero.setText(ph_number);

                if ( ph_number != null ) {

                    int value = db.deleteContact(ph_number);
                    if ( value != 0 ) {

                        cloud_db.collection("Utenti").document(email).collection("Contatti").document(sel_name)
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        System.out.println("Contatto eliminato correttamente") ;
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        System.out.println("Error deleting document: " + e);
                                    }
                                });
                        Toast.makeText(getApplicationContext(), "CONTATTO ELIMINATO", Toast.LENGTH_SHORT).show();
                    }

                    else {

                        Toast.makeText(getApplicationContext(), "CONTATTO INESISTENTE", Toast.LENGTH_SHORT).show();
                    }

                }

                System.out.println( db.getAllContacts() ) ;
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        loadContacts();
    }

    private void loadContacts() {

        ContentResolver contentResolver=getContentResolver();
        Cursor cursor=contentResolver.query(ContactsContract.Contacts.CONTENT_URI,null,null,null,null);

        if (cursor.moveToFirst()){
            do {

                String id = cursor.getString( cursor.getColumnIndex(ContactsContract.Contacts._ID)) ;
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)) ;
                System.out.println(name) ;
                nomi.add(name);
                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) ;

                if ( hasPhoneNumber > 0 ) {

                    Cursor cursor2 = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? ",
                            new String[]{id}, null) ;

                    while ( cursor2.moveToNext()) {
                        String phoneNumber = cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ;
                        numeri.add(phoneNumber) ;
                    }
                    cursor2.close() ;
                }

            } while( cursor.moveToNext()) ;
        }
        cursor.close() ;
        dataAdapter.notifyDataSetChanged();

    }
}