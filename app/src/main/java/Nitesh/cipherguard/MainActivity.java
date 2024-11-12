package Nitesh.cipherguard;

import static android.Manifest.*;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@RequiresApi(api = Build.VERSION_CODES.R)
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_BUTTON1 = 1;
    private static final int REQUEST_CODE_BUTTON2 = 2;
    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final String PERMISSION_MANAGE_EXTERNAL_STORAGE = permission.MANAGE_EXTERNAL_STORAGE;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_PERMISSION_GRANTED = "permissionGranted";
    private Button b1; //selectFileButton
    private Button b2;// decryption
    private String Key;// password
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;
    ProgressBar Pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check if permission is already granted
        if (!isPermissionGranted()) {
            // Permission is not granted, request it
            requestPermission();
        }
        Button b1 = findViewById(R.id.button1); //selectFileButton
        Button b2 = findViewById(R.id.button2);
        Pb=findViewById(R.id.progressBar);
        b1.setOnClickListener(v -> selectFile(REQUEST_CODE_BUTTON1));
        b2.setOnClickListener(v -> selectFile(REQUEST_CODE_BUTTON2));


    }
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{PERMISSION_MANAGE_EXTERNAL_STORAGE},
                REQUEST_CODE_PERMISSION);
    }

    private void savePermissionGranted() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_PERMISSION_GRANTED, true);
        editor.apply();
    }

    private boolean isPermissionGranted() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean(PREF_PERMISSION_GRANTED, false);
    }


    private void selectFile(int requestCode) {
        Pb.setVisibility(View.VISIBLE);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_BUTTON1 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                PasswordInput(uri, REQUEST_CODE_BUTTON1);//take password from user to encrypt and Key
            }
        }
        if (requestCode == REQUEST_CODE_BUTTON2 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                PasswordInput(uri, REQUEST_CODE_BUTTON2);//take password from user to encrypt and Key
            }
        }

    }
    private void PasswordInput(Uri uri, int type) { //InputPassword
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your Password");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the input
                String enteredText = input.getText().toString();
                // Do something with the enteredText
                Key=enteredText;
                if(type==REQUEST_CODE_BUTTON1) {
                    EncryptAndSaveFile(uri); //call to encryption; encryption is call in popup
                }
                if(type==REQUEST_CODE_BUTTON2)
                    DecryptAndSaveFile(uri);//
                Pb.setVisibility(View.INVISIBLE);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Pb.setVisibility(View.INVISIBLE);
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void EncryptAndSaveFile(Uri uri) {  //encryption
        try {
//            InputStream inputStream = getContentResolver().openInputStream(uri);
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            SecretKey secretKey = generateSecretKey(Key.toCharArray());
//            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
//            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
//
//            StringBuilder stringBuilder = new StringBuilder();
//
//            byte[] buffer = new byte[8192];
//            int count;
//            while ((count = inputStream.read(buffer)) > 0) {
//                byte[] encryptedBytes = cipher.update(buffer, 0, count);
//                stringBuilder.append(encryptedBytes);
//            }
//            byte[] encryptedBytes = cipher.doFinal(stringBuilder.toString().getBytes());
//
//            OutputStream outputStream = getContentResolver().openOutputStream(uri);
//            outputStream.write(encryptedBytes);
//            outputStream.close();
            //Progress_widget.setVisibility(View.VISIBLE);

            InputStream inputStream = getContentResolver().openInputStream(uri);

            SecretKey secretKey = generateSecretKey(Key.toCharArray());
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int count;
            while ((count = inputStream.read(buffer)) > 0) {
                byte[] encryptedBytes = cipher.update(buffer, 0, count);
                outputStream.write(encryptedBytes);
            }
            byte[] encryptedBytes = cipher.doFinal();
            outputStream.write(encryptedBytes);

            OutputStream fileOutputStream = getContentResolver().openOutputStream(uri);
            fileOutputStream.write(outputStream.toByteArray());
            fileOutputStream.close();

            //Pb.setVisibility(View.VISIBLE);
            Toast.makeText(this, "File encryption successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error encrypting file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void DecryptAndSaveFile(Uri uri){
        try{
            InputStream inputStream = getContentResolver().openInputStream(uri);

            SecretKey secretKey = generateSecretKey(Key.toCharArray());
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[8192];
            int count;
            while ((count = inputStream.read(buffer)) > 0) {
                byte[] decryptedBytes = cipher.update(buffer, 0, count);
                outputStream.write(decryptedBytes);
            }
            byte[] finalDecryptedBytes = cipher.doFinal(); // Process any remaining bytes
            outputStream.write(finalDecryptedBytes);

            OutputStream fileOutputStream = getContentResolver().openOutputStream(uri);
            fileOutputStream.write(outputStream.toByteArray());
            fileOutputStream.close();
            inputStream.close(); // Close the input stream



            Toast.makeText(this, "File decryption successfully!", Toast.LENGTH_SHORT).show();

        } catch (javax.crypto.BadPaddingException | javax.crypto.IllegalBlockSizeException e) {

            Toast.makeText(this, "Incorrect password. Decryption failed.", Toast.LENGTH_SHORT).show();
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error decrypting file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSION) {
            // Check if permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, save the status
                savePermissionGranted();
                // Proceed with your logic
                // You can do further initialization here
            } else {
                // Permission is denied, show a message or dialog
                showPermissionDeniedDialog();
                savePermissionGranted();
            }
        }
    }
    // Method to show a dialog when permission is denied
    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app requires access to manage all files. Please grant the permission in settings.");

        // Add a button to open app settings so the user can manually grant the permission
        builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                openPermissionSettings();
            }
        });
        // Show the dialog
        builder.show();
    }
    // Method to open app settings
    private void openPermissionSettings() {
//        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }


    private static SecretKey generateSecretKey(char[] password) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, "salt".getBytes(StandardCharsets.UTF_8), ITERATIONS, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
}
