package fun.xiantiao.maimaicontrol.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    // 程序界面宽度
    public final int WIDTH = 530;
    // 程序界面高度
    public final int HEIGHT = 390;

    // 数据显示区
    private final JTextArea mDataView = new JTextArea();
    private final JScrollPane mScrollDataView = new JScrollPane(mDataView);

    public MainFrame() {
        initView();
        initComponents();
    }

    /**
     * 初始化窗口
     */
    private void initView() {
        // 关闭程序
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        // 禁止窗口最大化
        setResizable(false);

        // 设置程序窗口居中显示
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        setBounds(p.x - WIDTH / 2, p.y - HEIGHT / 2, WIDTH, HEIGHT);
        this.setLayout(null);

        setTitle("串口通信");
    }

    /**
     * 初始化控件
     */
    private void initComponents() {
        // 数据显示
        mDataView.setFocusable(false);
        mScrollDataView.setBounds(10, 10, 505, 200);
        add(mScrollDataView);
    }

}
