rewrite tweetnacl.c in pure Java


### API/Usage

#### Public key authenticated encryption

* get key pair: Box.KeyPair kp = Box.keyPair(), kp = Box.keyPair_fromSecretKey(sk)
* new Box object: Box box = new Box(theirPublicKey, mySecretKey, Nonce);
* encryption: cipher = box.box(message);
* decryption: message = box.open(cipher);
* Nonce MUST be unique for ever message passed between same peers


#### Secret key authenticated encryption

* get shared key: crypto random, what you have
* new SecretBox object: SecretBox sbox = new SecretBox(sharedKey, Nonce);
* encryption: cipher = sbox.box(message);
* decryption: message = sbox.open(cipher);
* Nonce MUST be unique for ever message passed between same peers


### Signature

* get key pair: Signature.KeyPair kp = Signature.keyPair(), kp = Signature.keyPair_fromSecretKey(sk);
* new Signature object: Signature sig = new Signature(theirPublicKey, mySecretKey);
* sign: signedMessage = sig.sign(message);
* verify: message = sig.open(signedMessage);
* Nonce MUST be unique for ever message passed between same peers


### Hash

* generate SHA-512: byte [] tag = Hash.sha512(message);


### Refer to com.iwebpp.crypto.tests for details


### License MIT

