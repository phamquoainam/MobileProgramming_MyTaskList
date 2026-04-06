package hcmute.edu.vn.mytasklist.service.contact;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import java.util.ArrayList;
import java.util.List;

public class ContactContentHelper {

    public static class ContactItem {
        private String name;
        private String phoneNumber;

        public ContactItem(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
        }

        public String getName() { return name; }
        public String getPhoneNumber() { return phoneNumber; }
    }

    public static List<ContactItem> scanContacts(Context context) {
        List<ContactItem> contacts = new ArrayList<>();
        
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIndex);
                String number = cursor.getString(numberIndex);
                // Basic cleanup of phone numbers
                if (number != null) {
                    number = number.trim();
                }
                contacts.add(new ContactItem(name, number));
            }
            cursor.close();
        }
        return contacts;
    }
}
