/* Copyright (C) 2011, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import edu.ku.brc.specifydroid.R;

/**
 * @author rods
 *
 * @code_status Alpha
 *
 * Jan 21, 2011
 *
 */
public class DialogHelper
{
    public enum DlgType {close, yesno};
    
    private static String packageName = "edu.ku.brc.specifydroid";
    
    /**
     * @return the packageName
     */
    public static String getPackageName()
    {
        return packageName;
    }

    /**
     * @param packageName the packageName to set
     */
    public static void setPackageName(String packageName)
    {
        DialogHelper.packageName = packageName;
    }

    /**
     * @param activity
     * @param format
     * @param args
     */
    public static void showDialog(final Activity activity, final String format, Object...args)
    {
        String str = String.format(format, args);
        showDialog(activity, str);
    }
    
    /**
     * @param activity
     * @param resId
     * @param args
     */
    public static void showDialog(final Activity activity, final int resId, Object...args)
    {
        String str = activity.getString(resId, args);
        showDialog(activity, str);
    }
    
    /**
     * @param activity
     * @param strResName
     * @return
     */
    public static String getStringResourceByName(final Activity activity, final String strResName)
    {
      int resId = activity.getResources().getIdentifier(strResName, "string", packageName);
      return resId == 0 ? strResName : activity.getString(resId);
    }
    
    /**
     * @param activity
     * @param str
     */
    public static void showDialog(final Activity activity, final String str)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(str).setCancelable(false).setPositiveButton(activity.getString(R.string.close), null);
        activity.runOnUiThread(new Runnable() {
            public void run() {
                final AlertDialog alert = builder.create();
                alert.show();
            }});
    }
    
    /**
     * @param activity
     * @param id
     */
    public static void showDialog(final Activity activity, final int id)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(id).setCancelable(false).setPositiveButton(activity.getString(R.string.close), null);
        activity.runOnUiThread(new Runnable() {
            public void run() {
                final AlertDialog alert = builder.create();
                alert.show();
            }});
    }
    
    /**
     * @param action
     * @return
     */
    private static DialogInterface.OnClickListener getDefaultAction(final DialogInterface.OnClickListener action)
    {
        if (action != null)
        {
            return action;
        }
        return new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id)
            {
                dialog.cancel();
            }
        };
    }

    /**
     * @param activity
     * @param id
     */
    public static void showDialog(final Activity activity, 
                                  final DlgType type, 
                                  final int id,
                                  final DialogInterface.OnClickListener posAction,
                                  final DialogInterface.OnClickListener negAction)
    {
        if (type == DlgType.close)
        {
            showDialog(activity, id);
            return;
        }
        
        int posBtnId = R.string.yes;
        int negBtnId = R.string.no;
        
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(id)
                .setCancelable(false)
                .setPositiveButton(posBtnId, getDefaultAction(posAction))
                .setNegativeButton(negBtnId, getDefaultAction(negAction));
        final AlertDialog alert = builder.create();
        alert.show();

        activity.runOnUiThread(new Runnable() {
            public void run() {
                alert.show();
            }});
    }
    
    /**
     * @param activity
     * @param secondsDelay
     * @param resId
     */
    public static void showTimedDialog(final Activity activity,
                                       final double secondsDelay,
                                       final Integer resId,
                                       final TimedDialogListener listener)
    {
        showTimedDialog(activity, secondsDelay, resId, null, listener);
    }
    
    /**
     * @param activity
     * @param secondsDelay
     * @param text
     */
    public static void showTimedDialog(final Activity activity,
                                       final double secondsDelay, 
                                       final String text,
                                       final TimedDialogListener listener)
    {
        showTimedDialog(activity, secondsDelay, null, text, listener);
    }
    
    /**
     * @param activity
     * @param secondsDelay
     * @param resId
     * @param text
     */
    public static void showTimedDialog(final Activity activity,
                                       final double secondsDelay, 
                                       final Integer resId, 
                                       final String text,
                                       final TimedDialogListener listener)
    {
        String msg = resId != null ? activity.getString(resId) : text;
        
        InputMethodManager inputManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null && activity.getCurrentFocus() != null && activity.getCurrentFocus().getWindowToken() != null)
        {
            inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); 
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(msg);
        builder.setCancelable(false);
        
        final AlertDialog dlg = builder.create();
        dlg.show();
        
        new Thread() 
        {
            public void run() 
            {
                 try
                 {
                     int milliSecsSleep = (int)Math.round(secondsDelay * 1000.0);
                     Log.d("XXX", "Sleeping "+milliSecsSleep);
                     Thread.sleep(milliSecsSleep);
                     
                 } catch (Exception e) 
                 {  
                 } finally
                 {
                     activity.runOnUiThread(new Runnable() {
                         public void run() {
                             if (listener != null)
                             {
                                 Log.d("XXX", "closing dialog.");
                                 listener.dialogClosed();
                             }
                             dlg.dismiss();
                             }
                         });
                 }
            }
       }.start();
    }
    
    /**
     *
     */
    public interface TimedDialogListener
    {
        public void dialogClosed();
    }

}
