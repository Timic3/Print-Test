package si.timic.tests.test1;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;

import mjson.Json;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    final String TAG = "Printer";

    private static int REQUEST_SIGNIN = 1;

    final String CLIENT_ID = "<CLIENT_ID>";
    final String CLIENT_SECRET = "<CLIENT_SECRET";

    private TextView status;
    private TextView callback;
    Button printGCPButton;
    Button printWifiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.status);
        callback = (TextView) findViewById(R.id.callback);
        printGCPButton = (Button) findViewById(R.id.printGCPButton);
        printWifiButton = (Button) findViewById(R.id.printWifiButton);
        callback.setMovementMethod(new ScrollingMovementMethod());

        Log.d(TAG, "Initializing...");
        mAuth = FirebaseAuth.getInstance();
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(CLIENT_ID)
                .requestEmail()
                .requestServerAuthCode(CLIENT_ID)
                .requestScopes(new Scope("https://www.googleapis.com/auth/cloudprint"))
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File (sdCard.getAbsolutePath() + "/pdf");
        File f = new File(directory, "test.pdf");
        if(f.exists()) {
            Toast.makeText(this, "Exists!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Nope, doesn't exist", Toast.LENGTH_LONG).show();
        }
        //byte[] byteArray = new byte[(int) f.length()];

        printGCPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        printWifiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.setText("Getting printer via WiFi...");
                WifiAsync wi = new WifiAsync();
                callback.setText("Executing async task...");
                wi.execute();
                callback.setText("Executed!");
                if (wi.getException() != null) {
                    callback.setText(wi.getException().toString());
                } else {
                    callback.setText("Packet sent.");
                }
            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:"+user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Error connecting API: "+connectionResult.getErrorMessage());
        Toast.makeText(this, "Error (Connection) #1", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == REQUEST_SIGNIN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed, update UI appropriately
                // ...
                Toast.makeText(this, "Error (Request Code) #2 "+result.getStatus(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private class WifiAsync extends AsyncTask<Void, Void, Void> {

        private Exception exception;

        protected Void doInBackground(Void... params) {
            try {
                //Socket sock = new Socket("192.168.1.252", 9100);
                // Text
                String str = "Hello World!";
                Socket sock = new Socket("192.168.1.252", 9100);
                PrintWriter ou = new PrintWriter(sock.getOutputStream());
                ou.println(str);
                ou.flush();
                ou.close();
                sock.close();
                /*File sdCard = Environment.getExternalStorageDirectory();
                File directory = new File (sdCard.getAbsolutePath() + "/pdf");
                File f = new File(directory, "test.pdf");
                if(f.exists()) {
                    Log.d(TAG, "File exists yay!");
                }
                byte[] byteArray = new byte[(int) f.length()];
                FileInputStream fis = new FileInputStream(f);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(byteArray, 0, byteArray.length);
                //OutputStream os = sock.getOutputStream();
                PrintWriter ou = new PrintWriter(sock.getOutputStream());
                //ou.println(byteArray, 0, byteArray.length);
                ou.println("Hello World!");
                ou.println(new String(byteArray, "UTF-8"));
                ou.flush();
                ou.close();
                bis.close();
                //os.close();
                fis.close();
                sock.close();*/
                //fis.close();
            } catch(Exception e) {
                this.exception = e;
            }
            return null;
        }

        protected void onPostExecute(Void void1) {
            //callback.setText("Async task finished!");
        }

        WifiAsync() {

        }

        Exception getException() {
            return exception;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_SIGNIN);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount acc) {
        Log.d(TAG, "firebaseAuthWithGoogle:"+acc.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acc.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        //Log.d(TAG, "currentUser"+mAu)

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        FirebaseUser user = task.getResult().getUser();
                        status.setText("User: " + user.getEmail());
                        callback.setText("Authenticating with Cloud Print...");
                        //callback.setText("User is now: " + user.getEmail());
                        //Toast.makeText(MainActivity.this, "It works! User: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed #3",
                                    Toast.LENGTH_SHORT).show();
                        }
                        //callback.setText("Past task");
                        getAccess(acc.getServerAuthCode());
                    }
                });
    }

    private void getPrinters(final String token) {
        Log.d(TAG, "TOKEN: "+token);
        String url = "https://www.google.com/cloudprint/search";
        callback.setText("Getting printers...");
        Ion.with(this)
                .load("GET", url)
                .addHeader("Authorization", "Bearer " + token)
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        Log.d(TAG, "finished " + result.getHeaders().code() + ": " +
                                result.getResult());
                        callback.setText("Sending request to printer...");
                        //Json j = Json.read(result.getResult());
                        printPdf("", "d3ca4127-fb1d-d039-55f0-aa619c344830", token);
                        if (e == null) {
                            Log.d(TAG, "Sucess!");
                        } else {
                            Log.d(TAG, "Error :(");
                            //callback.setText("");
                        }
                    }
                });
    }

    private void getAccess(String code) {
        String url = "https://www.googleapis.com/oauth2/v4/token";
        //callback.setText("Ion exec");
        Ion.with(this)
                .load("POST", url)
                .setBodyParameter("client_id", CLIENT_ID)
                .setBodyParameter("client_secret", CLIENT_SECRET)
                .setBodyParameter("code", code)
                .setBodyParameter("grant_type", "authorization_code")
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        Log.d(TAG, "Result: " + result.getResult());
                        //callback.setText("");
                        if (e == null) {
                            try {
                                JSONObject json = new JSONObject(result.getResult());
                                getPrinters(json.getString("access_token"));
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Callback received null",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void printPdf(String pdfPath, String printerId, String token) {
        String url = "https://www.google.com/cloudprint/submit";
        callback.setText("Sending print request to printer...");
        Ion.with(this)
            .load("POST", url)
            .addHeader("Authorization", "Bearer " + token)
            //.setMultipartParameter("xsrf", xsrfToken)
            .setMultipartParameter("printerid", printerId)
            .setMultipartParameter("title", "print test")
            .setMultipartParameter("ticket", getTicket())
            //.setMultipartFile("content", "application/pdf", new File(pdfPath))
            .setMultipartParameter("content", "Hello World!")
            .setMultipartParameter("contentType", "text/plain")
            .asString()
            .withResponse()
            .setCallback(new FutureCallback<Response<String>>() {
                @Override
                public void onCompleted(Exception e, Response<String> result) {
                    if (e == null) {
                        Log.d(TAG, "PRINTTT CODE: " + result.getHeaders().code() +
                                ", RESPONSE: " + result.getResult());
                        callback.setText("Printed!");
                        /*Json j = Json.read(result.getResult());
                        if (j.at("success").asBoolean()) {
                            Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                        }*/
                    } else {
                        Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
                        Log.d(TAG, e.toString());
                    }
                }
            });
    }

    private String getTicket() {
        Json ticket = Json.object();
        Json print = Json.object();
        ticket.set("version", "1.0");

        print.set("vendor_ticket_item", Json.array());
        print.set("color", Json.object("type", "STANDARD_MONOCHROME"));
        print.set("copies", Json.object("copies", 1));

        ticket.set("print", print);
        return ticket.toString();
    }
}