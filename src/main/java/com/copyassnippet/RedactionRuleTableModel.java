package com.copyassnippet;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

class RedactionRuleTableModel extends AbstractTableModel {
    private final List<RedactionRule> rules = new ArrayList<>();

    void setRules(List<RedactionRule> newRules) {
        rules.clear();
        rules.addAll(newRules);
        fireTableDataChanged();
    }

    List<RedactionRule> getRules() {
        return new ArrayList<>(rules);
    }

    @Override
    public int getRowCount() {
        return rules.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int col) {
        return col == 0 ? "Type" : "Pattern";
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return true;
    }

    @Override
    public Object getValueAt(int row, int col) {
        RedactionRule rule = rules.get(row);
        return col == 0 ? rule.getType().displayName() : rule.getPattern();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        RedactionRule rule = rules.get(row);
        if (col == 0) {
            rule.setType(RedactionRule.Type.fromDisplayName((String) value));
        } else {
            rule.setPattern((String) value);
        }
        fireTableCellUpdated(row, col);
    }

    void addRule(RedactionRule rule) {
        rules.add(rule);
        fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
    }

    void removeRule(int row) {
        rules.remove(row);
        fireTableRowsDeleted(row, row);
    }
}
