package test

@SpringApplicationConfiguration(classes=TestConfiguration)
@WebAppConfiguration
@IntegrationTest('server.port:0')
@DirtiesContext
class IntegrationTests {

  @Value('${local.server.port}')
  int port

  private String home() {
    HttpHeaders headers = new HttpHeaders()
    headers.setAccept([MediaType.TEXT_HTML])
    new TestRestTemplate().exchange('http://localhost:' + port, HttpMethod.GET, new HttpEntity<Void>(headers), String).body
  }
  
  @Test
  void homeIsForm() {
    String body = home()
    assertTrue('Wrong body:\n' + body, body.contains('action="/starter.zip"'))
  }
  
  @Test
  void homeIsJson() {
    String body = new TestRestTemplate().getForObject('http://localhost:' + port, String)
    assertTrue('Wrong body:\n' + body, body.contains('{"styles"'))
  }
  
  @Test
  void webIsAddedPom() {
    String body = new TestRestTemplate().getForObject('http://localhost:' + port + '/pom.xml?packaging=war', String)
    assertTrue('Wrong body:\n' + body, body.contains('spring-boot-starter-web'))
    assertTrue('Wrong body:\n' + body, body.contains('provided'))
  }
  
  @Test
  void webIsAddedGradle() {
    String body = new TestRestTemplate().getForObject('http://localhost:' + port + '/build.gradle?packaging=war', String)
    assertTrue('Wrong body:\n' + body, body.contains('spring-boot-starter-web'))
    assertTrue('Wrong body:\n' + body, body.contains('providedRuntime'))
  }
  
  @Test
  void infoHasExternalProperties() {
    String body = new TestRestTemplate().getForObject('http://localhost:' + port + '/info', String)
    assertTrue('Wrong body:\n' + body, body.contains('"project"'))
  }
  
  @Test
  void homeHasWebStyle() {
    String body = home()
    assertTrue('Wrong body:\n' + body, body.contains('name="style" value="web"'))
  }

  @Test
  void homeHasBootVersion() {
    String body = home()
    assertTrue('Wrong body:\n' + body, body.contains('name="bootVersion" value="1'))
  }

  @Test
  void downloadStarter() {
    byte[] body = new TestRestTemplate().getForObject('http://localhost:' + port + 'starter.zip', byte[])
    assertNotNull(body)
    assertTrue(body.length>100)
  }
  
  @Test
  void installer() {
    ResponseEntity<String> response = new TestRestTemplate().getForEntity('http://localhost:' + port + 'install.sh', String)
    assertEquals(HttpStatus.OK, response.getStatusCode())
    assertNotNull(response.body)
  }
  
}

// CLI compiled classes are not @ComponentScannable so we have to create
// an explicit configuration for the test
@Configuration
@Import([app.MainController, app.Projects, app.TemporaryFileCleaner])
class TestConfiguration { 
}