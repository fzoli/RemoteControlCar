package chrriis.dj.nativeswing.swtimpl.components;

public interface MenuItemActionListener<T extends JMenuItem> {
    
    public void onSelected(MenuItemActionEvent<T> e);
    
}
