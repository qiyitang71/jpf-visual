import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

public class DropDownList implements ActionListener {
	public void actionPerformed(ActionEvent e) {
		JComboBox cb = (JComboBox) e.getSource();
		CheckComboStore store = (CheckComboStore) cb.getSelectedItem();
		CheckComboRenderer ccr = (CheckComboRenderer) cb.getRenderer();
		if (store == null)
			return;
		store.state = !store.state;
		ccr.checkBox.setSelected(store.state);
	}

	public JPanel getContent() {
		String[] ids = { "Class.field", "Class.method" };
		// Boolean[] values = { Boolean.TRUE, Boolean.FALSE, Boolean.FALSE,
		// Boolean.FALSE };
		CheckComboStore[] stores = new CheckComboStore[ids.length];
		ImageIcon removeIcon = new ImageIcon("remove.png");
		Map<String, CheckComboStore> map = new HashMap<>();
		for (int j = 0; j < ids.length; j++) {
			stores[j] = new CheckComboStore(ids[j], Boolean.FALSE);
			map.put(ids[j], stores[j]);
		}
		JComboBox combo = new JComboBox(stores) {
			@Override
			public Dimension getMaximumSize() {
				Dimension max = super.getMaximumSize();
				max.height = getPreferredSize().height;
				return max;
			}

		};
		combo.setAlignmentX(0);
		combo.setAlignmentY(0);
		combo.setRenderer(new CheckComboRenderer(removeIcon, combo, map));
		combo.addActionListener(this);
		combo.setEditable(true);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(combo);
		return panel;
	}

	public static void main(String[] args) {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().add(new DropDownList().getContent());
		f.setSize(300, 160);
		f.setLocation(200, 200);
		f.setVisible(true);
	}
}

/** adapted from comment section of ListCellRenderer api */
class CheckComboRenderer implements ListCellRenderer {
	JPanel panel;
	JCheckBox checkBox;
	JButton button;
	ImageIcon removeIcon;
	Integer idx;

	public CheckComboRenderer(ImageIcon ricon, final JComboBox combo, Map<String, CheckComboStore> map) {

		panel = new JPanel(new BorderLayout());
		checkBox = new JCheckBox();
		removeIcon = ricon;
		button = new JButton(removeIcon);
		button.setPreferredSize(new Dimension(removeIcon.getIconWidth(), removeIcon.getIconHeight()));
		button.setBorderPainted(false);
		panel.add(checkBox);
		panel.add(button, BorderLayout.EAST);
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (button.getX() < e.getX()) {
					System.out.println("button contains the click remove the item" + e.getComponent());
					if (combo.getComponentCount() > 0) {
						combo.removeItem(map.get(checkBox.getText()));
					}
				}
			}
		});
	}

	boolean isFirst = true;

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		if (isFirst) {
			isFirst = false;
			list.addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {
					panel.dispatchEvent(e);
					e.consume();
				}
			});
		}
		this.idx = index;

		CheckComboStore store = (CheckComboStore) value;
		if (value == null)
			return panel;

		checkBox.setText(store.id);

		checkBox.setSelected(((Boolean) store.state).booleanValue());

		button.setIcon(removeIcon);

		if (isSelected) {
			panel.setBackground(list.getSelectionBackground());
			panel.setForeground(list.getSelectionForeground());
		} else {
			panel.setBackground(list.getBackground());
			panel.setForeground(list.getForeground());
		}
		return panel;
	}
}

class CheckComboStore {
	String id;
	Boolean state;

	public CheckComboStore(String id, Boolean state) {
		this.id = id;
		this.state = state;
	}
}