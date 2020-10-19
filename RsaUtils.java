import org.apache.commons.codec.binary.Base64;
import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/*
 * @Author wadreamer
 * @Description //TODO RSA 工具类，公私钥生成，加解密，该类的对象不在 Spring 容器中
 * @Date 16:39 2020/10/16
 * @Param 
 * @return 
 **/
public class RsaUtils {

    private static final String SRC = "123456";

    public static void main(String[] args) throws Exception {
        System.out.println("\n");
        RsaKeyPair keyPair = generateKeyPair();
        System.out.println("公钥：" + keyPair.getPublicKey());
        System.out.println("私钥：" + keyPair.getPrivateKey());
        System.out.println("\n");
        test1(keyPair);
        System.out.println("\n");
        test2(keyPair);
        System.out.println("\n");
    }

    /*
     * @Author wadreamer
     * @Description //TODO 公钥加密，私钥解密
     * @Date 9:18 2020/10/19
     * @Param [keyPair]
     * @return void
     **/
    private static void test1(RsaKeyPair keyPair) throws Exception {
        System.out.println("***************** 公钥加密私钥解密开始 *****************");
        String text1 = encryptByPublicKey(keyPair.getPublicKey(), RsaUtils.SRC);
        String text2 = decryptByPrivateKey(keyPair.getPrivateKey(), text1);
        System.out.println("加密前：" + RsaUtils.SRC);
        System.out.println("加密后：" + text1);
        System.out.println("解密后：" + text2);
        if (RsaUtils.SRC.equals(text2)) {
            System.out.println("解密字符串和原始字符串一致，解密成功");
        } else {
            System.out.println("解密字符串和原始字符串不一致，解密失败");
        }
        System.out.println("***************** 公钥加密私钥解密结束 *****************");
    }

    /*
     * @Author wadreamer
     * @Description //TODO 私钥加密，公钥解密
     * @Date 9:19 2020/10/19
     * @Param [keyPair]
     * @return void
     **/
    private static void test2(RsaKeyPair keyPair) throws Exception {
        System.out.println("***************** 私钥加密公钥解密开始 *****************");
        String text1 = encryptByPrivateKey(keyPair.getPrivateKey(), RsaUtils.SRC);
        String text2 = decryptByPublicKey(keyPair.getPublicKey(), text1);
        System.out.println("加密前：" + RsaUtils.SRC);
        System.out.println("加密后：" + text1);
        System.out.println("解密后：" + text2);
        if (RsaUtils.SRC.equals(text2)) {
            System.out.println("解密字符串和原始字符串一致，解密成功");
        } else {
            System.out.println("解密字符串和原始字符串不一致，解密失败");
        }
        System.out.println("***************** 私钥加密公钥解密结束 *****************");
    }

   /*
    * @Author wadreamer
    * @Description //TODO 使用私钥加密
    * @Date 17:47 2020/10/16
    * @Param [privateKeyText, text]
    * @return java.lang.String
    **/
    public static String encryptByPrivateKey(String privateKeyText, String text) throws Exception {

        // 解码私钥字符串，得到私钥的 ASN.1 编码格式
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyText));

        // 构建 RSA 的秘钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        // 利用 RSA 秘钥工厂和 PKCS8EncodedKeySpec 生成私钥
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);

        // 创建 RSA 的 Cipher 对象，并提供加密功能
        Cipher cipher = Cipher.getInstance("RSA");

        // Cipher对象需要初始化
        // ENCRYPT_MODE 加密模式
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        // 得到需要加密后的二进制数据
        byte[] result = cipher.doFinal(text.getBytes());

        // 返回 Base64 编码后的加密字符串
        return Base64.encodeBase64String(result);
    }

    /*
     * @Author wadreamer
     * @Description //TODO 使用私钥解密
     * @Date 17:42 2020/10/16
     * @Param [privateKeyText, text]
     * @return java.lang.String
     **/
    public static String decryptByPrivateKey(String privateKeyText, String text) throws Exception {

        // 解码私钥字符串，得到私钥的 ASN.1 编码格式
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec5 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyText));

        // 构建 RSA 的秘钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        // 利用 RSA 秘钥工厂和 PKCS8EncodedKeySpec 生成私钥
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec5);

        // 创建 RSA 的 Cipher 对象，并供解密功能提
        Cipher cipher = Cipher.getInstance("RSA");
        
        // Cipher对象需要初始化
        // DECRYPT_MODE 解密模式
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // Base64.decodeBase64(text) 解码密文，从而得到解密后的的二进制数据
        byte[] result = cipher.doFinal(Base64.decodeBase64(text));
        
        // 返回解密后的数据
        return new String(result);
    }

    /*
     * @Author wadreamer
     * @Description //TODO 使用公钥加密
     * @Date 17:15 2020/10/16
     * @Param [publicKeyText, text]
     * @return java.lang.String
     **/
    public static String encryptByPublicKey(String publicKeyText, String text) throws Exception {

        // 解码公钥字符串，得到公钥的 ASN.1 编码格式
        X509EncodedKeySpec x509EncodedKeySpec2 = new X509EncodedKeySpec(Base64.decodeBase64(publicKeyText));

        // 构建 RSA 的秘钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        // 利用 RSA 秘钥工厂和 X509EncodedKeySpec 生成公钥
        PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec2);

        // 创建 RSA 的 Cipher 对象，并提供加密功能
        Cipher cipher = Cipher.getInstance("RSA");

        // Cipher对象需要初始化
        // ENCRYPT_MODE 加密模式
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // 得到需要加密后的二进制数据
        byte[] result = cipher.doFinal(text.getBytes());

        // 返回 Base64 编码后的加密字符串
        return Base64.encodeBase64String(result);
    }

    /*
     * @Author wadreamer
     * @Description //TODO 使用公钥解密
     * @Date 17:49 2020/10/16
     * @Param [publicKeyText, text]
     * @return java.lang.String
     **/
    public static String decryptByPublicKey(String publicKeyText, String text) throws Exception {

        // 解码公钥字符串，得到公钥的 ASN.1 编码格式
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKeyText));

        // 构建 RSA 的秘钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        // 利用 RSA 秘钥工厂和 X509EncodedKeySpec 生成公钥
        PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);

        // 创建 RSA 的 Cipher 对象，并提供解密功能
        Cipher cipher = Cipher.getInstance("RSA");

        // Cipher对象需要初始化
        // DECRYPT_MODE 解密模式
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        // Base64.decodeBase64(text) 解码密文，从而得到解密后的的二进制数据
        byte[] result = cipher.doFinal(Base64.decodeBase64(text));

        // 返回解密后的数据
        return new String(result);
    }

    /*
     * @Author wadreamer
     * @Description //TODO 构建 RSA 密钥对
     * @Date 16:41 2020/10/16
     * @Param []
     * @return me.zhengjie.utils.RsaUtils.RsaKeyPair
     **/
    public static RsaKeyPair generateKeyPair() throws NoSuchAlgorithmException {

        // 实例化秘钥生成器
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        // 设置秘钥位数
        keyPairGenerator.initialize(1024);

        // 生成秘钥对
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // 公钥对象
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();

        // 私钥对象
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

        // 以 Base64 对公钥进行编码
        String publicKeyString = Base64.encodeBase64String(rsaPublicKey.getEncoded());

        // 以 Base64 对私钥进行编码
        String privateKeyString = Base64.encodeBase64String(rsaPrivateKey.getEncoded());
        
        // 返回秘钥对象
        return new RsaKeyPair(publicKeyString, privateKeyString);
    }


    /*
     * @Author wadreamer
     * @Description //TODO RSA 密钥对对象
     * @Date 16:52 2020/10/16
     * @Param 
     * @return 
     **/
    public static class RsaKeyPair {

        private final String publicKey;
        private final String privateKey;

        public RsaKeyPair(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

    }
}
