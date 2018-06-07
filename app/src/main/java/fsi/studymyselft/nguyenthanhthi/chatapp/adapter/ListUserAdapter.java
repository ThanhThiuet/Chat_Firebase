package fsi.studymyselft.nguyenthanhthi.chatapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Random;

import fsi.studymyselft.nguyenthanhthi.chatapp.R;
import fsi.studymyselft.nguyenthanhthi.chatapp.data.model.Dialog;
import fsi.studymyselft.nguyenthanhthi.chatapp.data.model.Message;
import fsi.studymyselft.nguyenthanhthi.chatapp.data.model.User;
import fsi.studymyselft.nguyenthanhthi.chatapp.other.DrawableHelper;

/**
 * Created by thanhthi on 04/05/2018.
 */

public class ListUserAdapter extends BaseAdapter {

    private static final String TAG = "ListUserAdapter";

    private ArrayList<User> users;
    private LayoutInflater inflater;

    private LinearLayout messageRecentLayout;
    private TextView txtAvatar, txtEmail, txtMessageRecent, txtPosition;

    private Dialog myDialog;
    private Message recentMessage;

    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference rootReference, dialogsReference, myDialogReference, messagesReference;

    public ListUserAdapter(Context context, ArrayList<User> users) {
        inflater = LayoutInflater.from(context);
        this.users = users;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_list_users, null);
        }

        txtAvatar = convertView.findViewById(R.id.txtAvatar);
        txtEmail = convertView.findViewById(R.id.txtEmail);
        txtMessageRecent = convertView.findViewById(R.id.txtRecentMessage);
        txtPosition = convertView.findViewById(R.id.txtPosition);
        messageRecentLayout = convertView.findViewById(R.id.message_recent_layout);

        txtAvatar.setText(users.get(position).getEmail().substring(0, 1).toUpperCase());
        txtEmail.setText(users.get(position).getEmail());

//        setMessageRecent(users.get(position));

        if (recentMessage == null) {
            //visible child view
            Log.d(TAG, "recent message is null");
            txtMessageRecent.setText("");
        }
        else {
            txtMessageRecent.setText(recentMessage.getText());
        }

        //set color background for avatar
        DrawableHelper.withContext(convertView.getContext())
                .customColor(getRandomColor())
                .withDrawable(R.drawable.bg_avatar)
                .customTint()
                .applyToBackground(txtAvatar);

        return convertView;
    }

    private String getRandomColor() {
        String colorResult;
        Random random = new Random();
        int color = Color.argb(155, random.nextInt(256), random.nextInt(256), random.nextInt(256));
        colorResult = "#" + Integer.toHexString(color);
        return colorResult;
    }

    private void setMessageRecent(final User otherUser) {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //set reference of my dialog in database and get messages in this dialog
        final String dialogName = (currentUser.getUid() + "|" + otherUser.getId());
        final String reverseDialogName = (otherUser.getId() + "|" + currentUser.getUid());
        myDialog = new Dialog();
        myDialog.setName(dialogName);

        //get reference of root database
        database = FirebaseDatabase.getInstance();
        rootReference = database.getReference();

        //get reference of Dialog Database
        if (rootReference.child("Dialogs") == null) {
            rootReference.setValue("Dialogs");
        }
        dialogsReference = rootReference.child("Dialogs");

        dialogsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isMyDialogExist = false;

                if (dataSnapshot.exists()) { //if dialog database is exist
                    for (DataSnapshot dialogSnapshot : dataSnapshot.getChildren()) {
                        Dialog dialog = dialogSnapshot.getValue(Dialog.class);

                        //check existence of my dialog
                        if (dialog.getName().equals(dialogName) || dialog.getName().equals(reverseDialogName)) {
                            if (dialog.getName().equals(reverseDialogName)) {
                                myDialog.setName(reverseDialogName); //rename of my dialog if necessary
                            }
                            Log.d(TAG, "my dialog id = " + dialog.getId());
                            myDialog.setId(dialog.getId());
                            isMyDialogExist = true;
                            break;
                        }
                    }
                }

                if (!isMyDialogExist) {
                    return;
                }

                myDialogReference = dialogsReference.child(myDialog.getId());

                //get all messages in my dialog database
                getAllMessagesDialog(otherUser);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getAllMessagesDialog(final User otherUser) {
        if (myDialogReference.child("Messages") == null) {
            myDialogReference.setValue("Messages");
        }
        messagesReference = myDialogReference.child("Messages");

        messagesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Total count of messages in my dialog database = " + dataSnapshot.getChildrenCount());

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Message message = snapshot.getValue(Message.class);
                        myDialog.addMessageToListMessages(message);
                    }

                    Log.d(TAG, "Total count of messages in my Dialog = " + myDialog.getMessages().size());
                    recentMessage = myDialog.getMessages().get(myDialog.getMessages().size() - 1);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}