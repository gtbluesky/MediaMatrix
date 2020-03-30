precision mediump float;
uniform sampler2D uTextureUnit;
varying vec2 vTextureCoord;
const int GAUSSIAN_SAMPLES = 9;
uniform vec2 uVecStep;
uniform float uWeight[GAUSSIAN_SAMPLES];

void main() {
    vec3 color = vec3(0.0);
    vec4 fragColor = texture2D(uTextureUnit, vTextureCoord);

    for (int i = 0; i < GAUSSIAN_SAMPLES; ++i) {
        vec2 blurCoordinate = vTextureCoord + uVecStep * float(i - GAUSSIAN_SAMPLES / 2);
        color += texture2D(uTextureUnit, blurCoordinate).rgb * uWeight[i];
    }

    gl_FragColor = vec4(color, fragColor.a);
}