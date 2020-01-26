package me.aap.fermata.media.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.aap.fermata.R;
import me.aap.fermata.media.lib.MediaLib.BrowsableItem;
import me.aap.fermata.media.lib.MediaLib.Folders;
import me.aap.fermata.media.pref.FoldersPrefs;
import me.aap.fermata.pref.PreferenceStore;
import me.aap.fermata.pref.SharedPreferenceStore;
import me.aap.fermata.storage.MediaFile;
import me.aap.fermata.util.Utils;

import static me.aap.fermata.util.Utils.getResourceUri;
import static me.aap.fermata.util.Utils.mapToArray;

/**
 * @author Andrey Pavlenko
 */
class DefaultFolders extends BrowsableItemBase<FolderItem> implements Folders,
		FoldersPrefs {
	static final String ID = "Folders";
	private final DefaultMediaLib lib;
	private final SharedPreferenceStore foldersPrefStore;

	public DefaultFolders(DefaultMediaLib lib) {
		super(ID, null, null);
		this.lib = lib;
		SharedPreferences prefs = lib.getContext().getSharedPreferences("folders", Context.MODE_PRIVATE);
		foldersPrefStore = SharedPreferenceStore.create(prefs, getLib().getPrefs());
	}

	@NonNull
	@Override
	public String getTitle() {
		return getLib().getContext().getString(R.string.folders);
	}

	@NonNull
	@Override
	public String getSubtitle() {
		return "";
	}

	@NonNull
	@Override
	public DefaultMediaLib getLib() {
		return lib;
	}

	@Override
	public BrowsableItem getParent() {
		return null;
	}

	@NonNull
	@Override
	public PreferenceStore getParentPreferenceStore() {
		return getLib();
	}

	@NonNull
	@Override
	public BrowsableItem getRoot() {
		return this;
	}

	@Override
	public FoldersPrefs getPrefs() {
		return this;
	}

	@NonNull
	@Override
	public PreferenceStore getFoldersPreferenceStore() {
		return foldersPrefStore;
	}

	@Override
	public Collection<ListenerRef<Listener>> getBroadcastEventListeners() {
		return getLib().getBroadcastEventListeners();
	}

	@Override
	public MediaDescriptionCompat.Builder getMediaDescriptionBuilder() {
		return super.getMediaDescriptionBuilder()
				.setIconUri(getResourceUri(getLib().getContext(), R.drawable.folder));
	}

	public List<FolderItem> listChildren() {
		DefaultMediaLib lib = getLib();
		String[] pref = getFoldersPref();
		boolean preferFile = getPreferFileApiPref();
		List<FolderItem> children = new ArrayList<>(pref.length);
		StringBuilder sb = Utils.getSharedStringBuilder();
		sb.append(FolderItem.SCHEME).append(':');
		int len = sb.length();

		for (String uri : pref) {
			Uri u = Uri.parse(uri);
			MediaFile folder = MediaFile.create(u, preferFile);
			sb.setLength(len);
			sb.append(folder.getName());
			FolderItem i = FolderItem.create(sb.toString(), this, folder, lib);
			children.add(i);
		}

		return children;
	}

	@Override
	public boolean isFoldersItemId(String id) {
		int idx = id.indexOf(':');
		if (idx == -1) return false;

		switch (id.substring(0, idx)) {
			case FileItem.SCHEME:
			case FolderItem.SCHEME:
			case CueItem.SCHEME:
			case CueTrackItem.SCHEME:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void addItem(Uri uri) {
		List<FolderItem> children = getChildren(null);
		if (Utils.contains(children, u -> uri.equals(u.getFile().getUri()))) return;

		List<FolderItem> newChildren = new ArrayList<>(children.size() + 1);
		newChildren.addAll(children);
		newChildren.add(toFolderItem(uri));
		setChildren(newChildren);
		saveChildren(newChildren);
	}

	@Override
	public void removeItem(int idx) {
		List<FolderItem> newChildren = new ArrayList<>(getChildren(null));
		newChildren.remove(idx);
		setChildren(newChildren);
		saveChildren(newChildren);
	}

	@Override
	public void moveItem(int fromPosition, int toPosition) {
		List<FolderItem> newChildren = new ArrayList<>(getChildren(null));
		Utils.move(newChildren, fromPosition, toPosition);
		setChildren(newChildren);
		saveChildren(newChildren);
	}

	@Override
	public void updateSorting() {
		super.updateSorting();
		saveChildren(getChildren(null));
	}

	private void saveChildren(List<FolderItem> children) {
		setFoldersPref(mapToArray(children, i -> i.getFile().getUri().toString(), String[]::new));
	}

	private FolderItem toFolderItem(Uri u) {
		MediaFile folder = MediaFile.create(u, getPreferFileApiPref());
		StringBuilder sb = Utils.getSharedStringBuilder();
		sb.append(FolderItem.SCHEME).append(':').append(folder.getName());
		return FolderItem.create(sb.toString(), this, folder, getLib());
	}
}