/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package face;

import com.digitalpersona.onetouch.DPFPCaptureFeedback;
import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPImageQualityAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPImageQualityEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Array;
import java.sql.ResultSetMetaData;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import netscape.javascript.JSObject;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import org.bytedeco.opencv.global.opencv_imgproc;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import util.ConectaBanco;

/**
 *
 * @author Way4ward
 */
public class RecognizeFace extends javax.swing.JFrame {
private static final long serialVersionUID = 3389476239431661943L;
private DPFPCapture capturer = DPFPGlobal.getCaptureFactory().createCapture();
      
protected static JSObject jso = null;
public static DefaultTableModel dtm; 
public static ResultSetMetaData md;
private DPFPVerification Verificador = DPFPGlobal.getVerificationFactory().createVerification();


    ModelPerson mod = new ModelPerson();
    ControlPerson cod = new ControlPerson();

    private RecognizeFace.DaemonThread myThread = null;

    //JavaCV 1.5.1
    VideoCapture webSource = null;
    Mat cameraImage = new Mat();
    CascadeClassifier cascade = new CascadeClassifier("C:\\photos\\haarcascade_frontalface_alt.xml");
    LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();

    BytePointer mem = new BytePointer();
    RectVector detectedFaces = new RectVector();

    //Vars
    String root, firstNamePerson, lastNamePerson, officePerson, dobPerson, telefone;
    //Social Info
    String facebook, insta, linkedin, git;
    int idPerson;

      
      
    //Utils
    ConectaBanco conecta = new ConectaBanco();


    /**
     * Creates new form RecognizeFace
     */
    public RecognizeFace() {
         super("Recognize | Face and Fingerprint Recognition");
        initComponents();
          recognizer.read("C:\\photos\\classifierLBPH.yml");
        recognizer.setThreshold(80);
        startCamera();
        
          this.addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				init();
				start();
			}
			public void componentHidden(ComponentEvent e) {
				stop();
			}

		});
		pack();
this.addWindowListener(new java.awt.event.WindowAdapter() {
    @Override
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
       stopCamera();
    }
});
      
    }

        
    
    protected void init()
	{

		capturer.addDataListener(new DPFPDataAdapter() {
			public void dataAcquired(final DPFPDataEvent e) {
				SwingUtilities.invokeLater(new Runnable() {	
                                    public void run() {
					makeReport("The fingerprint sample was captured.");
					setPrompt("Scan the same finger again.");
					process(e.getSample());
                                        
                                        
                                         
      
        try {
        check();
    } catch (Exception ex) {
        ex.printStackTrace();
    }
				}});
			}
		});
		capturer.addReaderStatusListener(new DPFPReaderStatusAdapter() {
			public void readerConnected(final DPFPReaderStatusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {	public void run() {
					setPrompt("Scan the Student's fingerprint.");
				}});
			}
			public void readerDisconnected(final DPFPReaderStatusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {	public void run() {
					setPrompt("Connect Fingerprint Reader.");
				}});
			}
		});
		capturer.addSensorListener(new DPFPSensorAdapter() {
			public void fingerTouched(final DPFPSensorEvent e) {
				SwingUtilities.invokeLater(new Runnable() {	public void run() {
					makeReport("The fingerprint reader was touched.");
				}});
			}
			public void fingerGone(final DPFPSensorEvent e) {
				SwingUtilities.invokeLater(new Runnable() {	public void run() {
					makeReport("The finger was removed from the fingerprint reader.");
				}});
			}
		});
		capturer.addImageQualityListener(new DPFPImageQualityAdapter() {
			public void onImageQuality(final DPFPImageQualityEvent e) {
				SwingUtilities.invokeLater(new Runnable() {	public void run() {
					if (e.getFeedback().equals(DPFPCaptureFeedback.CAPTURE_FEEDBACK_GOOD))
						makeReport("The quality of the fingerprint sample is good.");
					else
						makeReport("The quality of the fingerprint sample is poor.");
				}});
			}
		});
	}

   public  void check(){
     /**   
try{ 
 
   pstmt = conn.prepareStatement("SELECT * FROM DATA");
 
rs= pstmt.executeQuery();
 
int i=0;
while(rs.next()){
i++;
System.out.println("SQL:"+rs.getString(1)+"\n");

System.out.println("Contador:"+i+"\n");

byte templateBuffer[] = rs.getBytes("FINGER");
 
 
DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
 
setTemplate(referenceTemplate);

DPFPVerificationResult result = Verificador.verify(featuresverificacion, getTemplate());

if (result.isVerified()){
 
 JOptionPane ops1 = new JOptionPane("DATA RETRIEVE!",JOptionPane.INFORMATION_MESSAGE);
                    JDialog dialog1 = ops1.createDialog("GUEST DATA RETRIEVE!");
                    dialog1.setAlwaysOnTop(true); //<-- this line
                    dialog1.setModal(true);
                    
                    dialog1.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog1.setVisible(true);
                    
            fullname.setText(rs.getString("NAME"));
            byte[] img = rs.getBytes("IMAGE");
 gender.setText(rs.getString("GENDER"));
  department.setText(rs.getString("DEPARTMENT"));
                    
                    jLabel8.setVisible(true);
                    jLabel9.setVisible(true);
  
  

                    //Resize The ImageIcon
                    ImageIcon image = new ImageIcon(img);
                    Image im = image.getImage();
                    Image myImg = im.getScaledInstance(jLabel2.getWidth(), jLabel2.getHeight(),Image.SCALE_SMOOTH);
                    ImageIcon newImage = new ImageIcon(myImg);
                    jLabel2.setIcon(newImage);
                    stop();
               
				start();
                       }
       
                    }
                    }
                    catch (SQLException e) {
                    System.out.println("Finger print Error: "+e.getMessage());
                    e.printStackTrace();
                    
                    }
    **/
    }
   
   
   private DPFPEnrollment enrollment = DPFPGlobal.getEnrollmentFactory().createEnrollment();
	protected void process(DPFPSample sample)
	{
    
featuresinscripcion = extractFeatures(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);


featuresverificacion = extractFeatures(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);


if (featuresinscripcion != null){
try{
 
enrollment.addFeatures(featuresinscripcion);
 
Image image=convertSampleToBitmap(sample);
drawPicture(image);


}
catch (DPFPImageQualityException ex) {
System.err.println("Error: "+ex.getMessage());
}

finally {
 
switch(enrollment.getTemplateStatus()){
case TEMPLATE_STATUS_READY:  
 
setTemplate(enrollment.getTemplate());
        
break;

case TEMPLATE_STATUS_FAILED: 
enrollment.clear();
stop();
 
setTemplate(null);
JOptionPane.showMessageDialog(this, "Data Retrieve", "Data inspector", JOptionPane.ERROR_MESSAGE);
start();
break;
}
}
}
}
        public void setTemplate(DPFPTemplate template) {
DPFPTemplate old = this.template;
this.template = template;
firePropertyChange(TEMPLATE_PROPERTY, old, template);
}

		public DPFPFeatureSet featuresinscripcion;
public DPFPFeatureSet featuresverificacion;

public DPFPFeatureSet extractFeatures(DPFPSample sample, DPFPDataPurpose purpose){
DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
try {
return extractor.createFeatureSet(sample, purpose);
}
catch (DPFPImageQualityException e) {
return null;
}
}

	protected void start()
	{
            try{
		capturer.startCapture();
		setPrompt("Scan student's fingerprint");
            }catch(Exception e){
                 System.out.println("Connect you finger print");
            }
	}

	protected void stop()
	{
		capturer.stopCapture();
	}

	public void setStatus(String string) {
	//	prompt.setText(string);
	}
	public void setPrompt(String string) {
	//	prompt.setText(string);
	}
	public void makeReport(String string) {
//		log.append(string + "\n");
	}
        private DPFPTemplate template;
public static String TEMPLATE_PROPERTY = "template";    
public DPFPTemplate getTemplate() {
return template;
}

	public void drawPicture(Image image) {
		//picture.setIcon(new ImageIcon(
		//		image.getScaledInstance(picture.getWidth(), picture.getHeight(), Image.SCALE_DEFAULT)));

	}

	protected Image convertSampleToBitmap(DPFPSample sample) {
		return DPFPGlobal.getSampleConversionFactory().createImage(sample);
	}

	

	//For enrollment template, use *.fpt for file format
	//For verification feature, use *.fpp
	protected void writeFile(String filepath, byte[] data) {
		try {
			FileOutputStream out = new FileOutputStream(new File(filepath));
			out.write(data);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
        public static void notifyHTML(String result) {
		if(jso!=null)
			jso.call("updateFingerprintStatus", new String[] {result} );
	}
    
    
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        kGradientPanel1 = new keeptoo.KGradientPanel();
        label_name = new javax.swing.JLabel();
        kGradientPanel2 = new keeptoo.KGradientPanel();
        label_phone = new javax.swing.JLabel();
        kGradientPanel3 = new keeptoo.KGradientPanel();
        label_office = new javax.swing.JLabel();
        label_photo = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        txt_id_label = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        kGradientPanel5 = new keeptoo.KGradientPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(132, 242, 145)));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel4.setBackground(new java.awt.Color(255, 255, 255));
        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setForeground(new java.awt.Color(82, 82, 82));
        jLabel2.setText("Office:");
        jPanel4.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, -1, -1));

        jLabel3.setForeground(new java.awt.Color(82, 82, 82));
        jLabel3.setText("Fullname:");
        jPanel4.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, -1, -1));

        jLabel4.setForeground(new java.awt.Color(82, 82, 82));
        jLabel4.setText("Phone:");
        jPanel4.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 20, -1, -1));

        kGradientPanel1.setBackground(new java.awt.Color(255, 255, 255));
        kGradientPanel1.setkEndColor(new java.awt.Color(57, 114, 227));
        kGradientPanel1.setkFillBackground(false);
        kGradientPanel1.setkStartColor(new java.awt.Color(122, 227, 192));
        kGradientPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        kGradientPanel1.add(label_name, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 170, 30));

        jPanel4.add(kGradientPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 190, 30));

        kGradientPanel2.setBackground(new java.awt.Color(255, 255, 255));
        kGradientPanel2.setkEndColor(new java.awt.Color(57, 114, 227));
        kGradientPanel2.setkFillBackground(false);
        kGradientPanel2.setkStartColor(new java.awt.Color(122, 227, 192));
        kGradientPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        kGradientPanel2.add(label_phone, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 170, 30));

        jPanel4.add(kGradientPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 40, 190, 30));

        kGradientPanel3.setBackground(new java.awt.Color(255, 255, 255));
        kGradientPanel3.setkEndColor(new java.awt.Color(57, 114, 227));
        kGradientPanel3.setkFillBackground(false);
        kGradientPanel3.setkStartColor(new java.awt.Color(122, 227, 192));
        kGradientPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        kGradientPanel3.add(label_office, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 340, 30));

        jPanel4.add(kGradientPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 100, 390, 30));

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 90, 400, 140));

        label_photo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        jPanel1.add(label_photo, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 50, 360, 390));

        jLabel13.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(100, 100, 100));
        jLabel13.setText("Recognize Face");
        jPanel1.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        jPanel2.setBackground(new java.awt.Color(101, 199, 113));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel14.setBackground(new java.awt.Color(90, 68, 193));
        jLabel14.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel14.setText("ID Face");
        jPanel2.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 90, 40));

        txt_id_label.setBackground(new java.awt.Color(132, 242, 145));
        txt_id_label.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        txt_id_label.setForeground(new java.awt.Color(255, 255, 255));
        jPanel2.add(txt_id_label, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 0, 40, 40));

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 10, 700, 40));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(158, 158, 159));
        jLabel11.setText("Personal Information");
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 70, -1, -1));

        kGradientPanel5.setBackground(new java.awt.Color(255, 255, 255));
        kGradientPanel5.setkFillBackground(false);
        jPanel1.add(kGradientPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1100, 460));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1100, 470));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(RecognizeFace.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(RecognizeFace.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(RecognizeFace.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RecognizeFace.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new RecognizeFace().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private keeptoo.KGradientPanel kGradientPanel1;
    private keeptoo.KGradientPanel kGradientPanel2;
    private keeptoo.KGradientPanel kGradientPanel3;
    private keeptoo.KGradientPanel kGradientPanel5;
    private javax.swing.JLabel label_name;
    private javax.swing.JLabel label_office;
    private javax.swing.JLabel label_phone;
    private javax.swing.JLabel label_photo;
    private javax.swing.JLabel txt_id_label;
    // End of variables declaration//GEN-END:variables
  class DaemonThread implements Runnable {

        protected volatile boolean runnable = false;

        @Override
        public void run() {
            synchronized (this) {
                while (runnable) {
                    try {
                        if (webSource.grab()) {
                            webSource.retrieve(cameraImage);
                            Graphics g = label_photo.getGraphics();

                            Mat imageGray = new Mat();
                            cvtColor(cameraImage, imageGray, COLOR_BGRA2GRAY);

                            RectVector detectedFace = new RectVector();
                            cascade.detectMultiScale(imageGray, detectedFace, 1.1, 2, 0, new Size(150, 150), new Size(500, 500));

                            for (int i = 0; i < detectedFace.size(); i++) {
                                Rect dadosFace = detectedFace.get(i);
                                rectangle(cameraImage, dadosFace, new Scalar(0, 255, 0, 3), 3, 0, 0);
                                Mat faceCapturada = new Mat(imageGray, dadosFace);
                                opencv_imgproc.resize(faceCapturada, faceCapturada, new Size(160, 160));

                                IntPointer rotulo = new IntPointer(1);
                                DoublePointer confidence = new DoublePointer(1);
                                recognizer.predict(faceCapturada, rotulo, confidence);
                                int prediction = rotulo.get(0);
                                String nome;
                                nome = firstNamePerson;
  
                                if (prediction > 60) {
                                    rectangle(cameraImage, dadosFace, new Scalar(0, 156, 255, 3), 3, 0, 0);
                                    System.out.println(confidence.get(0));
                                     idPerson = prediction;
                                    
//                                    label_name.setText("Unknown User");
//                                    txt_id_label.setText("");
//                                    label_office.setText("");
//                                    label_phone.setText("");
//                                    sendMessage_btn.setText("Send Message");
//                                    facebook = "";
//                                    insta = "";
//                                    git = "";
//                                    linkedin = "";
                                    rec();
                                } else {
                                    rectangle(cameraImage, dadosFace, new Scalar(0, 255, 0, 3), 3, 0, 0);
                                    System.out.println(confidence.get(0));
                                    idPerson = prediction;
                                  rec();
                                  
                                }
                            }

                            imencode(".bmp", cameraImage, mem);
                            Image im = ImageIO.read(new ByteArrayInputStream(mem.getStringBytes()));
                            BufferedImage buff = (BufferedImage) im;

                            try {
                                if (g.drawImage(buff, 0, 0, 360, 390, 0, 0, buff.getWidth(), buff.getHeight(), null)) {
                                    if (runnable == false) {
                                        this.wait();
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    private void rec() {
        //Retrieve data from database
        new Thread() {
            @Override
            public void run() {
                conecta.conexao();
                try {
                    conecta.executaSQL("SELECT * FROM person WHERE id = " + String.valueOf(idPerson));
                    while (conecta.rs.next()) {
                        firstNamePerson = conecta.rs.getString("first_name");
                     
                        label_name.setText(conecta.rs.getString("first_name") + " " + conecta.rs.getString("last_name"));
                        label_office.setText(conecta.rs.getString("office"));
                        telefone = conecta.rs.getString("phone_number");
                        label_phone.setText(telefone);
                      
                        txt_id_label.setText(conecta.rs.getString("id"));

                        //Social Info
                        facebook = conecta.rs.getString("profile_facebook");
                        insta = conecta.rs.getString("profile_instagram");
                        linkedin = conecta.rs.getString("profile_linkedin");
                        git = conecta.rs.getString("profile_github");

                        Array ident = conecta.rs.getArray("first_name");
                        String[] person = (String[]) ident.getArray();

                        for (String person1 : person) {
                            System.out.println(person1);
                        }

                    }
                } catch (Exception e) {
                }
                conecta.desconecta();
            }
        }.start();
    }

    public void stopCamera() {
        try {
            myThread.runnable = false;
            webSource.release();
            dispose();
        } catch (Exception e) {
        }
    }

    public void startCamera() {
        new Thread() {
            @Override
            public void run() {
                webSource = new VideoCapture(0);
                myThread = new RecognizeFace.DaemonThread();
                Thread t = new Thread(myThread);
                t.setDaemon(true);
                myThread.runnable = true;
                t.start();
            }
        }.start();
    }
}