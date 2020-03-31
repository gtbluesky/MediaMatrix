varying highp vec2 vTextureCoord;
uniform sampler2D uTextureUnit;
uniform sampler2D uToneCurveTextureUnit;

void main() {
    lowp vec4 textureColor = texture2D(uTextureUnit, vTextureCoord);
    lowp float redCurveValue = texture2D(uToneCurveTextureUnit, vec2(textureColor.r, 0.0)).r;
    lowp float greenCurveValue = texture2D(uToneCurveTextureUnit, vec2(textureColor.g, 0.0)).g;
    lowp float blueCurveValue = texture2D(uToneCurveTextureUnit, vec2(textureColor.b, 0.0)).b;

    gl_FragColor = vec4(redCurveValue, greenCurveValue, blueCurveValue, textureColor.a);
}