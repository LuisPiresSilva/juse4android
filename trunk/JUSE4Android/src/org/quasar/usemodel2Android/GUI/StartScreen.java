package org.quasar.usemodel2Android.GUI;

import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.awt.*;
import java.awt.image.*;

import javax.imageio.*;
import javax.swing.*;

public class StartScreen extends JFrame{

	private Container cp;
	JLayeredPane lPane;

	public StartScreen() throws IOException{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screenSize);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setUndecorated(true);
		JPanel j = new ImageTestPanel();
		

		cp = getContentPane();
		setBackground(new Color(0, 0, 0, 0));
		setVisible(true);
		
		cp.add(j);
		
		setVisible(true);
		
		((ImageTestPanel) j).getl().get(0).alpha = 0;
		((ImageTestPanel) j).getl().get(0).repaint();
		((ImageTestPanel) j).getl().get(1).alpha = 0;
		((ImageTestPanel) j).getl().get(1).repaint();
		((ImageTestPanel) j).getl().get(2).alpha = 0;
		((ImageTestPanel) j).getl().get(2).repaint();
		((ImageTestPanel) j).getl().get(3).alpha = 0;
		((ImageTestPanel) j).getl().get(3).repaint();
		
		for(int i = 0;i < 100;i++){
			((ImageTestPanel) j).getl().get(0).alpha = i / 100f;
			((ImageTestPanel) j).getl().get(0).repaint();
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for(int i = 0;i < 100;i++){
			((ImageTestPanel) j).getl().get(1).alpha = i/ 100f;
			((ImageTestPanel) j).getl().get(1).repaint();
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for(int i = 0;i < 100;i++){
			((ImageTestPanel) j).getl().get(2).alpha = i/ 100f;
			((ImageTestPanel) j).getl().get(2).repaint();
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for(int i = 0;i < 100;i++){
			((ImageTestPanel) j).getl().get(3).alpha = i/ 100f;
			((ImageTestPanel) j).getl().get(3).repaint();
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		dispose();
	}
}

class ImageTestPanel extends JPanel  
{  
	BufferedImage[] pngs;
	BufferedImage[] jpgs;
	ArrayList<ImageLabel> list;
	ArrayList<ImageLabel> getl(){
		return list;
	}
	public BufferedImage[] getPngs(){
		return pngs;
	}
    public ImageTestPanel()  
    {  
        jpgs = loadImages();  
       	pngs = createNewImages(jpgs); 
       	list = new ArrayList<ImageLabel>();
       	makeLabels(getPngs());
        setBackground(new Color(0,0,0,0)); 
    }  
   
    void makeLabels(BufferedImage[] images)  
    {  
        setLayout(new GridBagLayout());  
        GridBagConstraints gbc = new GridBagConstraints();  
        gbc.weightx = 0;  
   
        for(int i = 0; i < images.length; i++)  
        {  
            ImageLabel label = new ImageLabel(images[i], 0.2f);  
            if(i > 0)  
            {  
            	int dx = 0;
            	if(i == 0){
            		dx = screenSize.width;
            		setOpaque(true);
            	}
            	if(i == 1){
            		dx = screenSize.width - (screenSize.width - 1000);//screen width - image width
            		setOpaque(false);
            	}
            	if(i == 2){
            		dx = screenSize.width - (screenSize.width - 1000);
            		setOpaque(false);
            	}
            	if(i == 3){
            		dx = screenSize.width - (screenSize.width - 1000);
            		setOpaque(false);
            	}
            	
                gbc.insets = new Insets(0, -dx, 0, 0);  
            } 
            add(label, gbc);
            list.add(label);
        }  
    }  
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private BufferedImage[] createNewImages(BufferedImage[] opaque)  
    {  
        BufferedImage[] transparent = new BufferedImage[4];  
   
        for(int i = 0; i < opaque.length; i++)  
        {  
            int w = opaque[i].getWidth();  
            int h = opaque[i].getHeight();  
            transparent[i] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);  
            Graphics2D g2 = transparent[i].createGraphics();  
            g2.drawImage(opaque[i], null, 0, 0);  
            g2.dispose();  
            
        }  
        return transparent;  
    }  
   
    private BufferedImage[] loadImages()  
    {  
        String[] fileNames = {  
            "guiImages/USE2Android1.png",
            "guiImages/USE2Android2.png",
            "guiImages/USE2Android3.png",
            "guiImages/USE2Android4.png"
        };  
        BufferedImage[] images = new BufferedImage[fileNames.length];  
        for(int i = 0; i < images.length; i++)  
            try  
            {  
//                URL url = getClass().getResource(fileNames[i]);  
                images[i] = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(fileNames[i]));  
            }  
            catch(MalformedURLException mue)  
            {  
                System.out.println("Bad URL: " + mue.getMessage());  
            }  
            catch(IOException ioe)  
            {  
                System.out.println("Read trouble: " + ioe.getMessage());  
            }  
            return images;  
    }  
}  
   
class ImageLabel extends JLabel  
{  
    BufferedImage image;  
    float alpha;  
   
    public ImageLabel(BufferedImage image, float alpha)  
    {  
        this.image = image;  
        this.alpha = alpha;  
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));  
    }  
   
    protected void paintComponent(Graphics g)  
    {  
        super.paintComponent(g);  
        Graphics2D g2 = (Graphics2D)g;  
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);  
        g2.setComposite(ac);  
        g2.drawImage(image, null, 0, 0);  
    }  
}  