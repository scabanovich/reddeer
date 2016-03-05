/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.reddeer.core.lookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.hamcrest.Matcher;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.ShellWithTextIsActive;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.core.handler.ToolItemHandler;
import org.jboss.reddeer.core.handler.WidgetHandler;
import org.jboss.reddeer.core.util.Display;
import org.jboss.reddeer.core.util.ResultRunnable;

/**
 * Menu lookup provides methods for finding menus and context menus and their items. Works also with dynamic menus.
 * 
 * @author Jiri Peterka
 * @author Rastislav Wagner
 * 
 */
@SuppressWarnings("unchecked")
public class MenuLookup {

	private static final Logger log = Logger.getLogger(MenuLookup.class);
	private static MenuLookup instance = null;
	
	private MenuLookup() { }
	
	/**
	 * Gets singleton instance of MenuLookup.
	 * 
	 * @return instance of MenuLookup
	 */
	public static MenuLookup getInstance() {
		if (instance == null) {
			instance = new MenuLookup();
		}
		return instance;
	}
	
	/**
	 * Gets all tool bar menu items.
	 *  
	 * @return list of MenuManager instances related to tool bar menus.
	 */
	public List<IContributionItem> getViewMenus(){	
		IWorkbenchPart part = getActivePart(false);
		
		List<IContributionItem> menuContributionItems = new ArrayList<IContributionItem>();
		IMenuManager m = ((IViewSite) part.getSite()).getActionBars().getMenuManager();
		if (m instanceof MenuManager) {
			menuContributionItems.addAll(Arrays.asList(((MenuManager) m).getItems()));
		}
		if(menuContributionItems.isEmpty()){
			throw new CoreLayerException("No Menu found in active part: "+part.getTitle());
		}
		return menuContributionItems;
	}
	
	/**
	 * Looks for ActionContributionItem matching matchers.
	 *
	 * @param contItems the cont items
	 * @param matchers menu item text matchers
	 * @return action contribution item
	 */
	public ActionContributionItem lookFor(final List<IContributionItem> contItems, final Matcher<String>... matchers) {	
		ActionContributionItem contItem = Display.syncExec(new ResultRunnable<ActionContributionItem>(){

			@Override
			public ActionContributionItem run() {
				ActionContributionItem currentItem = null;
				List<IContributionItem> currentMenuContributionItems = contItems;
				for (Matcher<String> m : matchers) {
					currentItem = null;
					for (IContributionItem i : currentMenuContributionItems) {
						if(i instanceof ActionContributionItem){
							String normalized = ((ActionContributionItem)i).getAction().getText().replace("&", "");
							log.debug("Found item:'" + normalized + "'");
							if (m.matches(normalized)) {
								log.info("Item match:'" + normalized + "'");
								currentItem =(ActionContributionItem)i;
								break;
							} 
						} else if(i instanceof MenuManager){
							String normalized =((MenuManager)i).getMenuText().replace("&", "");
							log.debug("Found Menu Manager:'" + normalized + "'");
							if (m.matches(normalized)) {
								log.debug("Menu Manager match:'" + normalized + "'");
								currentMenuContributionItems = Arrays.asList(((MenuManager) i).getItems());
							}
						}
					}
			
				}
				return currentItem;
			}
		});
		return contItem;
	}
	
	public MenuItem lookForViewMenu(final List<IContributionItem> contItems, final Matcher<String>... matchers) {
		IWorkbenchPart part = getActivePart(false);
		final IMenuManager m = ((IViewSite) part.getSite()).getActionBars().getMenuManager();
		if (!(m instanceof MenuManager)) {
			throw new CoreLayerException("No Menu found in active part: " + part.getTitle());
		}
		MenuItem contItem = Display.syncExec(new ResultRunnable<MenuItem>() {

			@Override
			public MenuItem run() {
				MenuItem currentItem = null;
				Menu currentMenu = ((MenuManager) m).getMenu();
				for (Matcher<String> matcher : matchers) {
					for (MenuItem item : currentMenu.getItems()) {
						String normalized = item.getText().replace("&", "");
						if (matcher.matches(normalized)) {

							// is submenu?
							if (item.getMenu() != null) {

								// populate submenu
								sendHide(item.getMenu(), true);
								sendShowUI(item.getMenu());

								// drill down
								currentMenu = item.getMenu();
								break;
							} else {
								
								// we got a match
								currentItem = item;
								break;
							}
						}
					}
				}
				return currentItem;
			}
		});
		return contItem;
	}
	
	/**
	 * Gets contribution items from focused control.
	 * Used in situation when menu could contain dynamic menu from e4.
	 * 
	 * @return list of menu contribution items
	 */
	public List<IContributionItem> getMenuContributionItems() {
		List<IContributionItem> contItems = new ArrayList<IContributionItem>();
		final Control control  = WidgetLookup.getInstance().getFocusControl();
		final Menu menu = getControlMenu(control);
		
		contItems = Display.syncExec(new ResultRunnable<List<IContributionItem>>() {
			@Override
			public List<IContributionItem> run() {
				List<IContributionItem> contItemsRun = new ArrayList<IContributionItem>();
				sendHide(menu, true);
				sendShowUI(menu);
				if(menu.getData() != null && menu.getData() instanceof MenuManager){
					contItemsRun.addAll(Arrays.asList(((MenuManager)menu.getData()).getItems()));
					log.debug("Menu manager found");
				} else {
					log.debug("Menu manager not found");
				}
	
				return contItemsRun;
			}
		});
		return contItems;
	}
	
	

	/**
	 * Looks for MenuItem matching matchers starting on specified array of top level menu items.
	 * 
	 * @param topItems top level menu items
	 * @param matchers menu item text matchers
	 * @return MenuItem matching matchers
	 */
	public MenuItem lookFor(MenuItem[] topItems, Matcher<String>... matchers) {		
		MenuItem lastMenuItem = getMatchingMenuPath(topItems, matchers);
		if (lastMenuItem == null) {
			throw new CoreLayerException("No menu item matching specified path found");
		}
		return lastMenuItem;
	}
	

	/**
	 * Gets top level menu items of focused control.
	 * Does not work with dynamic menus from e4 - see {@link #MenuLookup.getMenuContributionItems()}.
	 * 
	 * @return array of top menu items of focused control
	 */
	public MenuItem[] getTopMenuMenuItemsFromFocus() {

		final Control control  = WidgetLookup.getInstance().getFocusControl();
		if (control == null) {
			throw new CoreLayerException(
					"No control has focus. Perhaps something has stolen it? Try to regain focus with for example \"new DefaultShell()\".");
		}
		MenuItem[] items = null;
		
		//Send MenuDetect event. Some menus doesn't exist before that..
		WidgetHandler.getInstance().notify(SWT.MenuDetect, control);
		
		final Menu menu = getControlMenu(control);
		
		items = Display.syncExec(new ResultRunnable<MenuItem[]>() {
			@Override
			public MenuItem[] run() {
				sendHide(menu, true);
				sendShowUI(menu);				
				return menu.getItems();
			}
		});

		if (items == null) {
			throw new CoreLayerException(
					"Could not find top menu items, menu doesn't exist or wrong focus");
		}

		return items;
	}
	
	/**
	 * Gets menu items from active shell menu bar.
	 * @return array of top menu items of active shell
	 */
	public MenuItem[] getActiveShellTopMenuItems() {
		Shell activeShell = ShellLookup.getInstance().getActiveShell();
		if(activeShell == null){
			throw new CoreLayerException("Cannot find menu bar because there's no active shell");
		}
		String activeShellText = WidgetHandler.getInstance().getText(activeShell);
		MenuItem[] result = null;
		try{
			result = getMenuBarItems(activeShell);	
		} catch (CoreLayerException swtle) {
			// there is a chance that some non expected shell was opened
			// e.g. Progress Dialog
			new WaitWhile(new ShellWithTextIsActive(activeShellText),TimePeriod.NORMAL,false);
			activeShell = ShellLookup.getInstance().getActiveShell();
			if (!activeShellText.equals(WidgetHandler.getInstance().getText(activeShell))){
				result = getMenuBarItems(activeShell);
			} else{
				throw swtle;
			}
		}		
		return result;		
	}
	
	/**
	 * Gets first level of menu items for specified DropDown ToolItem.
	 * 
	 * @param item DropDown ToolItem to get its menu items
	 * @return first level of menu items
	 */

	public MenuItem[] getToolItemMenuItems(ToolItem item) {
		final ShowMenuListener l = new ShowMenuListener();
		addMenuListener(l);
		ToolItemHandler.getInstance().clickDropDown(item);
		removeMenuListener(l);
		return Display.syncExec(new ResultRunnable<MenuItem[]>() {
			@Override
			public MenuItem[] run() {
				l.getMenu().setVisible(false);
				return l.getMenu().getItems();
			}
		});
	}

	private void addMenuListener(final Listener listener) {
		Display.syncExec(new Runnable() {

			@Override
			public void run() {
				Display.getDisplay().addFilter(SWT.Show, listener);
			}
		});
	}

	private void removeMenuListener(final Listener listener) {
		Display.syncExec(new Runnable() {

			@Override
			public void run() {
				Display.getDisplay().removeFilter(SWT.Show, listener);
			}
		});
	}

	/**
	 * Gets menu bar items.
	 * 
	 * @param s shell where menu bar items are looked up
	 * @return array of menu items of specified shell 
	 */
	private MenuItem[] getMenuBarItems(final Shell s) {

		MenuItem[] items = Display.<MenuItem[]>syncExec(()-> {
			log.info("Getting Menu Bar of shell '" + s.getText() + "'");
			Menu menu = s.getMenuBar();
			return (menu == null) ? null : menu.getItems();
		});
		
		if(items == null){
			String shellText = WidgetHandler.getInstance().getText(s);
			throw new CoreLayerException("Cannot find a menu bar of shell " + shellText);
		}
		return items;
	}

	/**
	 * Gets Menu of specified control.
	 * 
	 * @param c control where menu is placed
	 * @return menu placed under specified control
	 */
	private Menu getControlMenu(final Control c) {

		Menu menu = Display.syncExec(new ResultRunnable<Menu>() {

			@Override
			public Menu run() {
				Menu m = c.getMenu();
				return m;
			}
		});

		if (menu == null) {
			throw new CoreLayerException(
					c.getClass() +" Has no menu");
		}

		return menu;	
	}
	
	/**
	 * Gets Menu item matching menu path defined by specified top menu items and matchers.
	 * 
	 * @param topItems top level menu items where to search for menu item
	 * @param matchers matchers to match menu item
	 * @return matching MenuItem
	 */
	private MenuItem getMatchingMenuPath(final MenuItem[] topItems,
			final Matcher<String>... matchers) {
		MenuItem i = Display.syncExec(new ResultRunnable<MenuItem>() {

			@Override
			public MenuItem run() {
				Menu currentMenu = null;
				MenuItem currentItem = null;;
				MenuItem[] menuItems = topItems;
				for (Matcher<String> m : matchers) {
					currentItem = null;
					for (MenuItem i : menuItems) {
						String normalized = i.getText().replace("&", "");
						log.debug("Found menu:'" + normalized + "'");
						if (m.matches(normalized)) {
							log.debug("Item match:" + normalized);
							currentItem = i;
							currentMenu = i.getMenu();
							break;
						} 
					}
					if (currentItem == null){
						return null;
					}
					if (m != matchers[matchers.length-1]) {
						currentMenu = currentItem.getMenu();
						sendShowUI(currentMenu);
						menuItems = currentMenu.getItems();
					} 
				}
				return currentItem;
			}
		});
		return i;
	}


	/**
	 * Sends SWT.Show to widget.
	 * 
	 * @param widget widget where event is sent
	 */
	private void sendShowUI(Widget widget) {
		widget.notifyListeners(SWT.Show, new Event());
	}
		

	/**
	 * Hides menu.
	 * 
	 * @param menu menu to hide
	 * @param recur recursion flag
	 */
	private void sendHide(final Menu menu, final boolean recur) {
		Display.syncExec(new Runnable() {

			@Override
			public void run() {

				if (menu != null) {
					menu.notifyListeners(SWT.Hide, new Event());
					if (recur) {
						if (menu.getParentMenu() != null) {
							sendHide(menu.getParentMenu(), recur);
						} else {
							menu.setVisible(false);
						}
					}
				}
			}

		});

	}

	/**
	 * Gets active Workbench Part.
	 * @param restore should restore the part
	 * @return active WorkbenchPart
	 */
	private IWorkbenchPart getActivePart(final boolean restore) {
		IWorkbenchPart result = Display.syncExec(new ResultRunnable<IWorkbenchPart>() {

			@Override
			public IWorkbenchPart run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
				IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
				IWorkbenchPartReference activePartReference = activePage.getActivePartReference();
				IWorkbenchPart part = activePartReference.getPart(restore);
				return part;
			}		
		});
		return result;		
	}

	private class ShowMenuListener implements Listener{

		private Menu menu = null;
		
		public Menu getMenu() {
			return menu;
		}
		
		@Override
		public void handleEvent(Event event) {
			if (event.widget instanceof Menu){
				Menu menu = (Menu) event.widget;
				if (event.type == SWT.Show){
					this.menu = menu;
				}
				if (event.type == SWT.Hide){
					this.menu = null;
				}
			}
		}
		
	}
	
}
