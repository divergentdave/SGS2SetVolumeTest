package dnc.setvolumetest;

import android.content.DialogInterface;

final public class DismissDialogListener implements DialogInterface.OnClickListener
{
	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		dialog.dismiss();
	}
}
